package me.pectics.paper.plugin.permpacks

import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.Pack
import me.pectics.paper.plugin.permpacks.data.PackItem
import me.pectics.paper.plugin.permpacks.data.UrlPackItem
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.pack.Packer
import me.pectics.paper.plugin.permpacks.upload.FileMetaRepository
import me.pectics.paper.plugin.permpacks.upload.UploadService
import me.pectics.paper.plugin.permpacks.util.logger
import me.pectics.paper.plugin.permpacks.util.sha1
import me.pectics.paper.plugin.permpacks.util.validate
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.net.URI

internal class Options private constructor(private val plugin: PermPacks) {

    private inner class FileConfigWrapper(val path: String) {

        val file = File(plugin.dataFolder, path)

        private lateinit var _config: FileConfiguration
        val config: FileConfiguration
            get() = _config

        private fun extract() = plugin.saveResource(path, true)

        fun load() {
            if (!file.exists()) extract()
            _config = YamlConfiguration.loadConfiguration(file)
        }

        fun reload() {
            if (!file.exists()) extract()
            _config.load(file)
        }

        fun boolean(path: String, def: Boolean) = _config.getBoolean(path, def)
        fun string(path: String) = _config.getString(path)
        fun section(path: String) = _config.getConfigurationSection(path)
    }

    private val log = logger("OptionLoader")

    private val configWrapper = FileConfigWrapper(CONFIG_FILE)
    private val packsWrapper = FileConfigWrapper(PACKS_FILE)
    private val wrappers = listOf(configWrapper, packsWrapper)

    private val _packs: MutableSet<Pack> = mutableSetOf()

    private var uploadServiceWarningShown = false

    init {
        // Extract and load configs
        wrappers.forEach(FileConfigWrapper::load)
    }

    fun loadPacks() {
        _packs.clear()
        UploadService.clearCache()
        FileMetaRepository.clear()
        uploadServiceWarningShown = false

        packsWrapper.config.getKeys(false)
            .mapNotNull(::loadPack)
            .forEach(_packs::add)

        Packer.cache(_packs)
    }

    private fun loadPack(id: String): Pack? {
        val section = packsWrapper.config.getConfigurationSection(id)
        if (section == null) {
            log.warning("Pack \"$id\" does not have a valid configuration section, skipped.")
            return null
        }

        val permission = section.getString("permission")
        if (permission.isNullOrBlank()) {
            log.warning("Pack \"$id\" does not have a permission set, skipped.")
            return null
        }

        val items = parseItems(section, id) ?: return null
        val priority = section.getInt("priority", 0)
        return Pack(id, permission, items, priority)
    }

    private fun parseItems(section: ConfigurationSection, packId: String): List<PackItem>? {
        val rawItems = section["items"] ?: run {
            log.warning("Pack \"$packId\" does not have a valid items list, skipped.")
            return null
        }

        val itemList = rawItems as? List<*>
        if (itemList == null) {
            log.warning("Pack \"$packId\" does not have a valid items list, skipped.")
            return null
        }

        val parsedItems = itemList.mapIndexedNotNull { index, raw ->
            parseItem(raw, packId, index)
        }

        if (parsedItems.isEmpty()) {
            log.warning("Pack \"$packId\" does not have any valid items, skipped.")
            return null
        }

        return parsedItems
    }

    private fun parseItem(rawItem: Any?, packId: String, index: Int): PackItem? {
        val item = rawItem as? Map<*, *>
        if (item == null) {
            log.warning("Items[$index] in pack \"$packId\" is invalid, skipped.")
            return null
        }

        val matches = ITEM_TYPE_KEYS.count(item::containsKey)
        if (matches != 1) {
            val reason = if (matches == 0) "missing all" else "has multiple"
            log.warning("Items[$index] in pack \"$packId\" $reason of $ITEM_TYPE_KEYS, skipped.")
            return null
        }

        val hash = (item["hash"] as? String)
            ?.let(Sha1Hex::of)
            ?.onFailure {
                log.warning("Items[$index] in pack \"$packId\" has an invalid hash, skipped.")
            }
            ?.getOrNull()

        return when {
            item["url"] is String -> createUrlItem(item["url"] as String, hash, packId, index)
            item["file"] is String -> createFileItem(item["file"] as String, hash, packId, index)
            else -> throw IllegalStateException("Mischecked pack item: $item")
        }
    }

    private fun createUrlItem(urlString: String, hash: Sha1Hex?, packId: String, index: Int): PackItem? {
        return runCatching { URI(urlString).toURL() }
            .onFailure {
                log.warning("Items[$index] in pack \"$packId\" has an invalid url, skipped.")
            }
            .getOrNull()
            ?.let { UrlPackItem(it, hash) }
    }

    private fun createFileItem(path: String, hash: Sha1Hex?, packId: String, index: Int): PackItem? {
        if (!UploadService.available()) {
            logUploadServiceUnavailable(packId)
            return null
        }

        val file = File(path)
            .runCatching {
                validate()
                this
            }
            .getOrElse {
                log.warning("Items[$index] in pack \"$packId\" refers to an invalid file, skipped.")
                return null
            }

        val computedHash = file.sha1()
        if (hash != null && hash != computedHash) {
            log.warning("Items[$index] in pack \"$packId\" has a mismatched hash, replaced with: $computedHash")
            return FilePackItem(file, computedHash)
        }

        return FilePackItem(file, hash ?: computedHash)
    }

    private fun logUploadServiceUnavailable(packId: String) {
        if (!uploadServiceWarningShown) {
            log.warning(
                "Pack \"$packId\" requires file upload service, but it's not configured properly, " +
                    "all items with local files will be skipped."
            )
            uploadServiceWarningShown = true
        }
    }

    companion object {

        private const val CONFIG_FILE = "config.yml"
        private const val PACKS_FILE = "packs.yml"
        private val ITEM_TYPE_KEYS = listOf("url", "file")

        private lateinit var instance: Options

        val packs: Set<Pack>
            get() = instance._packs
        val blockOtherPacks: Boolean
            get() = instance.configWrapper
                .boolean("block_other_packs", false)
        val fileUploadEnabled: Boolean
            get() = instance.configWrapper
                .boolean("file_upload.enabled", false)
        val fileUploadService: String?
            get() = instance.configWrapper
                .string("file_upload.service")
        val fileUploadContext: Map<String, Any>?
            get() = instance.configWrapper.let {
                val service = fileUploadService?.lowercase() ?: return null
                it.section("file_upload.$service")?.getValues(false)
            }

        fun initialize(plugin: PermPacks) {
            instance = Options(plugin)
        }

        fun loadPacks() = instance.loadPacks()

        fun reload() {
            instance.wrappers.forEach(FileConfigWrapper::reload)
            instance.loadPacks()
        }
    }
}

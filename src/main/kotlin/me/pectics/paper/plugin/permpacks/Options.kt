package me.pectics.paper.plugin.permpacks

import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.Pack
import me.pectics.paper.plugin.permpacks.data.UrlPackItem
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.pack.Packer
import me.pectics.paper.plugin.permpacks.upload.FileMetaRepository
import me.pectics.paper.plugin.permpacks.upload.UploadService
import me.pectics.paper.plugin.permpacks.util.logger
import me.pectics.paper.plugin.permpacks.util.sha1
import me.pectics.paper.plugin.permpacks.util.validate
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.net.URI

internal class Options private constructor(private val plugin: PermPacks) {

    private inner class FileConfigWrapper(val path: String) {

        val file = File(plugin.dataFolder, path)

        private lateinit var _config: FileConfiguration
        val config get() = _config

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

    private val configWrapper = FileConfigWrapper("config.yml")
    private val packsWrapper = FileConfigWrapper("packs.yml")
    private val wrappers = listOf(configWrapper, packsWrapper)

    private val _packs: MutableSet<Pack> = mutableSetOf()

    init {
        // Extract and load configs
        wrappers.forEach(FileConfigWrapper::load)
        // Read packs
        readPacks()
    }

    private fun readPacks() {
        _packs.clear()
        FileMetaRepository.clear()
        val packsConfig = packsWrapper.config

        var uploadServiceChecked = false

        for (id in packsConfig.getKeys(false)) {
            val section = packsConfig.getConfigurationSection(id)!! // Safe

            val permission = section["permission"] as? String ?: run {
                log.warning("Pack \"$id\" does not have a permission set, skipped.")
                continue
            }

            val priority = section["priority"] as? Int ?: 0

            val items = (section["items"] as? List<*>)
                ?.mapIndexedNotNull { i, item ->
                    // Validate item structure
                    // Must be a map and contain exactly one of the keys in list
                    val typeKeys = listOf("url", "file")
                    item as? Map<*,*> ?: run {
                        log.warning("Items[$i] in pack \"$id\" is invalid, skipped.")
                        return@mapIndexedNotNull null
                    }
                    val hits = typeKeys.count(item::containsKey)
                    if (hits != 1) {
                        val reason = if (hits == 0) "missing all" else "has multiple"
                        log.warning("Items[$i] in pack \"$id\" $reason of $typeKeys, skipped.")
                        return@mapIndexedNotNull null
                    }

                    // Optional hash
                    val hash = (item["hash"] as? String)
                        ?.let(Sha1Hex::of)
                        ?.onFailure {
                            log.warning("Items[$i] in pack \"$id\" has an invalid hash, skipped.")
                            return@mapIndexedNotNull null
                        }
                        ?.getOrNull()

                    // Create item
                    when {
                        item["url"] is String -> {
                            val url = URI(item["url"] as String).toURL()
                            UrlPackItem(url, hash)
                        }
                        item["file"] is String -> {
                            // Upload service availability check
                            if (!UploadService.available()) {
                                // Check only once
                                if (!uploadServiceChecked) {
                                    log.warning("Pack \"$id\" requires file upload service, but it's not configured properly, all items with local files will be skipped.")
                                    uploadServiceChecked = true
                                }
                                return@mapIndexedNotNull null
                            }

                            // Validate file
                            val file = File(item["file"] as String)
                                .runCatching { validate(); this }
                                .getOrElse {
                                    log.warning("Items[$i] in pack \"$id\" refers to an invalid file, skipped.")
                                    return@mapIndexedNotNull null
                                }

                            // If hash is provided, check it
                            val hash0 = file.sha1()
                            if (hash != null && hash != hash0) {
                                log.warning("Items[$i] in pack \"$id\" has a mismatched hash, replaced with: $hash0")
                                return@mapIndexedNotNull FilePackItem(file, hash0)
                            }
                            FilePackItem(file, hash ?: hash0)
                        }
                        else -> throw IllegalStateException("Mischecked pack item: $item")
                    }
                }
                ?.ifEmpty {
                    log.warning("Pack \"$id\" does not have any valid items, skipped.")
                    continue
                }
                ?: run {
                    log.warning("Pack \"$id\" does not have a valid items list, skipped.")
                    continue
                }

            _packs.add(Pack(id, permission, items, priority))
        }

        Packer.cache(_packs)
    }

    companion object {

        private lateinit var instance: Options

        val packs: Set<Pack> get() = instance._packs
        val blockOtherPacks: Boolean get() = instance.configWrapper
            .boolean("block_other_packs", false)
        val fileUploadEnabled: Boolean get() = instance.configWrapper
            .boolean("file_upload.enabled", false)
        val fileUploadService: String? get() = instance.configWrapper
            .string("file_upload.service")
        val fileUploadContext: Map<String, Any>? get() = instance.configWrapper.let {
            val service = fileUploadService?.lowercase() ?: return null
            it.section("file_upload.$service")?.getValues(false)
        }

        fun initialize(plugin: PermPacks) {
            instance = Options(plugin)
        }

        fun reload() {
            instance.wrappers.forEach(FileConfigWrapper::reload)
            instance.readPacks()
        }

    }

}
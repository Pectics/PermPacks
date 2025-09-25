package me.pectics.paper.plugin.permpacks.upload

import me.pectics.paper.plugin.permpacks.BinaryCache
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.util.SerializableURI
import me.pectics.paper.plugin.permpacks.util.cap
import java.io.File
import java.net.URI

internal abstract class UploadService {

    /**
     * Names to identify the service.
     */
    abstract val names: List<String>

    /**
     * Launch the service with the given context.
     */
    abstract fun launch(context: Map<String, Any>)

    /**
     * Shutdown the service.
     */
    abstract fun shutdown()

    /**
     * Upload the given file and return its URI.
     */
    abstract fun upload(file: File): URI

    /**
     * Validate the cached URI for the given item.
     */
    open fun validate(item: FilePackItem, cached: SerializableURI): Boolean = true

    protected inline fun <reified T> Map<String, Any>.parseTo(key: String): T =
        when (val value = this[key]) {
            is T -> value
            null -> throw IllegalArgumentException("${key.cap()} is not specified.")
            else -> throw IllegalArgumentException(
                "${key.cap()} must be ${T::class.simpleName}, but got: ${value::class.simpleName}"
            )
        }

    companion object {

        private lateinit var service: UploadService
        private val uploaded = mutableMapOf<Sha1Hex, SerializableURI>()

        fun available() = ::service.isInitialized

        fun initialize(service: UploadService, context: Map<String, Any>) {
            BinaryCache.get<Map<Sha1Hex, SerializableURI>>("uploaded_packs")
                ?.apply(uploaded::putAll)
            service.launch(context)
            this.service = service
        }

        fun shutdown() {
            if (available()) service.shutdown()
            uploaded.clear()
        }

        fun clearCache() {
            uploaded.clear()
            BinaryCache.remove("uploaded_packs")
        }

        fun urlOf(item: FilePackItem): URI {
            val hash = item.hash
            val cached = uploaded[hash]
            if (cached != null && service.validate(item, cached))
                return cached
            val url = service.upload(item.file)
            uploaded[hash] = url
            BinaryCache["uploaded_packs"] = uploaded
            return url
        }
    }
}
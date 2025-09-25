package me.pectics.paper.plugin.permpacks.upload

import me.pectics.paper.plugin.permpacks.BinaryCache
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.util.SerializableURI
import java.io.File
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

internal abstract class UploadService {

    /**
     * Names to identify the service.
     */
    abstract val names: List<String>

    /**
     * Launch the service with the given context.
     */
    abstract fun launch(context: UploadServiceContext)

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

    /**
     * Cleanup remote storage based on a set of allowed hashes.
     * Default no-op.
     */
    open fun cleanup(retain: Set<Sha1Hex>) {}

    companion object {

        private lateinit var service: UploadService
        private val uploaded = ConcurrentHashMap<Sha1Hex, SerializableURI>()
        private val locks = ConcurrentHashMap<Sha1Hex, Any>()

        fun available() = ::service.isInitialized

        fun initialize(service: UploadService, context: UploadServiceContext) {
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

            val lock = locks.computeIfAbsent(hash) { Any() }
            synchronized(lock) {
                val secondCheck = uploaded[hash]
                if (secondCheck != null && service.validate(item, secondCheck))
                    return secondCheck
                val url = service.upload(item.file)
                uploaded[hash] = url
                BinaryCache["uploaded_packs"] = uploaded
                return url
            }
        }

        fun cleanup(retain: Set<Sha1Hex>) {
            if (available()) service.cleanup(retain)
        }
    }
}
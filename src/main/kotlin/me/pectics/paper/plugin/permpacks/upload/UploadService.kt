package me.pectics.paper.plugin.permpacks.upload

import me.pectics.paper.plugin.permpacks.BinaryCache
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.util.SerializableURL
import java.io.File
import java.net.URL

internal interface UploadService {

    fun launch(context: Map<String, Any>)
    fun shutdown()

    fun upload(file: File): URL

    fun isCachedUrlValid(item: FilePackItem, cached: SerializableURL): Boolean = true

    companion object {

        private lateinit var service: UploadService
        private val uploaded = mutableMapOf<Sha1Hex, SerializableURL>()

        fun available() = ::service.isInitialized

        fun initialize(service: UploadService, context: Map<String, Any>) {
            this.service = service
            BinaryCache.get<Map<Sha1Hex, SerializableURL>>("uploaded_packs")
                ?.apply(uploaded::putAll)
            this.service.launch(context)
        }

        fun shutdown() {
            if (available()) service.shutdown()
            uploaded.clear()
        }

        fun clearCache() {
            uploaded.clear()
            BinaryCache.remove("uploaded_packs")
        }

        fun urlOf(item: FilePackItem): URL {
            val hash = item.hash
            val cached = uploaded[hash]
            if (cached != null && service.isCachedUrlValid(item, cached))
                return cached

            val url = service.upload(item.file)
            uploaded[hash] = url
            BinaryCache["uploaded_packs"] = uploaded
            return url
        }

    }

}
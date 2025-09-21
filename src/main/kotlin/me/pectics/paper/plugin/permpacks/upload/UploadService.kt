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

    companion object {

        private lateinit var service: UploadService

        private val uploaded = mutableMapOf<Sha1Hex, SerializableURL>()

        fun initialize(service: UploadService, context: Map<String, Any>) {
            this.service = service
            BinaryCache.get<Map<Sha1Hex, SerializableURL>>("uploaded_packs")
                ?.apply(uploaded::putAll)
            this.service.launch(context)
        }

        fun shutdown() {
            if (::service.isInitialized)
                service.shutdown()
            uploaded.clear()
        }

        fun urlOf(item: FilePackItem): URL {
            val hash = item.hash
            return uploaded.getOrPut(hash) {
                val url = service.upload(item.file)
                uploaded[hash] = url
                BinaryCache["uploaded_packs"] = uploaded
                return@getOrPut url
            }
        }

    }

}
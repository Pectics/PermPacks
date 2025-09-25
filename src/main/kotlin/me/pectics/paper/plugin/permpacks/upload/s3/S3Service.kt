package me.pectics.paper.plugin.permpacks.upload.s3

import me.pectics.paper.plugin.permpacks.upload.UploadService
import software.amazon.awssdk.services.s3.S3Client
import java.io.File
import java.net.URI

internal object S3Service : UploadService() {

    override val names = listOf("amazon_s3", "amazons3", "s3")

    private var _client: S3Client? = null
    private val client get() = _client ?: throw IllegalStateException("S3Service is not launched.")

    override fun launch(context: Map<String, Any>) {
        TODO("Not yet implemented")
    }

    override fun shutdown() {

    }

    override fun upload(file: File): URI {
        TODO("Not yet implemented")
    }
}
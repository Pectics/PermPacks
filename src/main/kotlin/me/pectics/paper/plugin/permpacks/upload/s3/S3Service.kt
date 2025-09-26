package me.pectics.paper.plugin.permpacks.upload.s3

import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.upload.UploadService
import me.pectics.paper.plugin.permpacks.upload.UploadServiceContext
import me.pectics.paper.plugin.permpacks.util.removePrefixIgnoreCase
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.core.sync.RequestBody
import me.pectics.paper.plugin.permpacks.util.sha1
import me.pectics.paper.plugin.permpacks.util.validate
import java.io.File
import java.net.URI
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

internal object S3Service : UploadService {

    override val names = listOf("amazon_s3", "amazons3", "s3")

    private var _client: S3Client? = null
    private val client get() = _client ?: throw IllegalStateException("S3Service is not launched.")

    private lateinit var bucket: String
    private lateinit var directory: String
    private lateinit var urlFormat: String

    @Suppress("HttpUrlsUsage")
    override fun launch(context: UploadServiceContext) {
        val endpoint = context.required("endpoint").to<String>()
            .removePrefixIgnoreCase("http://")
            .removePrefixIgnoreCase("https://")
            .let(::URI)
        val region = context.required("region").to<String>()
            .let(Region::of)

        val accessKeyId = context.required("access_key_id").to<String>()
        val secretAccessKey = context.required("secret_access_key").to<String>()
        val credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)

        val pathStyleAccess = context.required("path_style_access").to<Boolean>()
        val chunkedEncoding = context.required("chunked_encoding").to<Boolean>()

        // S3 client build
        _client = S3Client.builder()
            .endpointOverride(endpoint)
            .region(region)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .serviceConfiguration {
                it.pathStyleAccessEnabled(pathStyleAccess)
                it.chunkedEncodingEnabled(chunkedEncoding)
            }
            .build()

        // Check existence of the bucket
        bucket = context.required("bucket").to<String>()
        if (client.listBuckets().buckets().none { it.name() == bucket })
            throw IllegalArgumentException("Bucket does not exist: $this")

        // Trim and concat slash
        directory = context.optional("directory")?.to<String>()
            ?.trim { it == '/' || it.isWhitespace() }
            ?.takeIf(String::isNotBlank)
            ?.plus('/')
            ?: ""

        urlFormat = when (pathStyleAccess) {
            true -> "https://${endpoint.host}/$bucket/$directory%s"
            false -> "https://$bucket.${endpoint.host}/$directory%s"
        }
    }

    override fun shutdown() {
        _client?.close()
        _client = null
    }

    override fun upload(file: File): URI {
        file.validate()
        val hash = file.sha1().value
        val putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key("$directory$hash")
            .contentLength(file.length())
            .acl(ObjectCannedACL.PUBLIC_READ)
            .build()

        val response = client.putObject(
            putRequest,
            RequestBody.fromFile(file)
        ).sdkHttpResponse()
        if (!response.isSuccessful)
            throw IllegalStateException("Failed to upload file to S3: $response")

        val url = urlFormat.format(hash)
        return URI.create(url)
    }

    override fun cleanup(retain: Iterable<FilePackItem>) {
        // Build a set of string hashes for efficient membership tests
        val retainStrings = retain.mapTo(mutableSetOf()) { it.hash.value }

        var continuationToken: String? = null
        do {
            val listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(directory)
                .maxKeys(1000)
                .apply { if (continuationToken != null) continuationToken(continuationToken) }
                .build()

            val listRes = client.listObjectsV2(listReq)
            continuationToken = if (listRes.isTruncated) listRes.nextContinuationToken() else null

            listRes.contents().forEach { s3Object ->
                val key = s3Object.key()
                val hash = if (directory.isEmpty()) key else key.removePrefix(directory)
                // Only manage objects that look like our SHA-1 keys
                val looksLikeSha1 = hash.length == 40 && hash.all { it.isDigit() || it in 'a'..'f' }
                if (looksLikeSha1 && hash !in retainStrings) {
                    val delReq = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build()
                    client.deleteObject(delReq)
                }
            }
        } while (continuationToken != null)
    }
}
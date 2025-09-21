package me.pectics.paper.plugin.permpacks.upload.selfhost

import com.sun.net.httpserver.HttpServer
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.upload.FileMetaRepository
import me.pectics.paper.plugin.permpacks.upload.UploadService
import me.pectics.paper.plugin.permpacks.util.SerializableURL
import me.pectics.paper.plugin.permpacks.util.cap
import me.pectics.paper.plugin.permpacks.util.sha1
import me.pectics.paper.plugin.permpacks.util.validate
import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.net.URL

object SelfHostService : UploadService {

    private val server = HttpServer.create()

    private lateinit var host: String
    private var port: Int = -1

    private lateinit var urlFormat: String

    init {
        server.createContext("/", SelfHostHandler)
        server.executor = null
    }

    private inline fun <reified T> Map<String, Any>.parseTo(key: String): T = when (val value = this[key]) {
        is T -> value
        null -> throw IllegalArgumentException("${key.cap()} is not specified.")
        else -> throw IllegalArgumentException(
            "${key.cap()} must be ${T::class.simpleName}, but got: ${value::class.simpleName}"
        )
    }

    @Suppress("HttpUrlsUsage")
    override fun launch(context: Map<String, Any>) {
        host = context.parseTo("host")
        port = context.parseTo("port")
        urlFormat = "http://$host:$port/%s"
        server.bind(InetSocketAddress(port), 0)
        server.start()
    }

    override fun shutdown() {
        server.stop(0)
    }

    override fun upload(file: File): URL {
        file.validate()
        val hash = file.sha1()
        if (hash !in FileMetaRepository)
            FileMetaRepository.push(file)
        val url = urlFormat.format(FileMetaRepository[hash]!!.repoFile.name)
        return URI.create(url).toURL()
    }

    override fun isCachedUrlValid(item: FilePackItem, cached: SerializableURL): Boolean {
        return item.hash in FileMetaRepository
    }

}

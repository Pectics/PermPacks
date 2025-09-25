package me.pectics.paper.plugin.permpacks.upload.selfhost

import com.sun.net.httpserver.HttpServer
import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.domain.value.Host
import me.pectics.paper.plugin.permpacks.domain.value.Port
import me.pectics.paper.plugin.permpacks.upload.FileMetaRepository
import me.pectics.paper.plugin.permpacks.upload.UploadService
import me.pectics.paper.plugin.permpacks.upload.UploadServiceContext
import me.pectics.paper.plugin.permpacks.util.SerializableURI
import me.pectics.paper.plugin.permpacks.util.sha1
import me.pectics.paper.plugin.permpacks.util.validate
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import java.io.File
import java.net.InetSocketAddress
import java.net.URI

internal object SelfHostService : UploadService {

    override val names = listOf("selfhost", "self-host", "self_host")

    private var _server: HttpServer? = null
    private val server get() = _server ?: throw IllegalStateException("HttpServer of SelfHostService is not launched.")

    private var host = Host.UNINITIALIZED
    private var port = Port.UNINITIALIZED

    private lateinit var urlFormat: String

    @Suppress("HttpUrlsUsage")
    override fun launch(context: UploadServiceContext) {
        host = context.required("host").to<String>()
            .let(Host::of)
            .getOrThrow()
        port = context.required("port").to<Int>()
            .let(Port::of)
            .getOrThrow()
        urlFormat = "http://$host:$port/%s"
        _server = HttpServer.create()
        server.apply {
            createContext("/", SelfHostHandler)
            executor = null
            bind(InetSocketAddress(port.value.toInt()), 0)
        }
        server.start()
    }

    override fun shutdown() {
        _server?.stop(0)
        _server = null
    }

    override fun upload(file: File): URI {
        file.validate()
        val hash = file.sha1()
        if (hash !in FileMetaRepository)
            FileMetaRepository.push(file)
        val url = urlFormat.format(FileMetaRepository[hash]!!.repoFile.name)
        return URI.create(url)
    }

    override fun validate(item: FilePackItem, cached: SerializableURI): Boolean {
        return item.hash in FileMetaRepository
    }

    override fun cleanup(retain: Set<Sha1Hex>) {
        FileMetaRepository.cleanup(retain)
    }
}

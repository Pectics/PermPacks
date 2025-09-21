package me.pectics.paper.plugin.permpacks.upload.selfhost

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.upload.FileMetaRepository

internal object SelfHostHandler : HttpHandler {

    override fun handle(ex: HttpExchange) {
        // Method filter, GET only
        if (ex.requestMethod != "GET") {
            ex.sendResponseHeaders(405, -1)
            ex.close()
            return
        }

        // Path filter, /{hash} only
        val hash = ex.requestURI.path
            ?.removePrefix("/")
            ?.let(Sha1Hex::of)
            ?.getOrNull()
            ?: run {
                ex.sendResponseHeaders(404, -1)
                ex.close()
                return
            }

        // Lookup file meta
        val meta = FileMetaRepository[hash] ?: run {
            ex.sendResponseHeaders(404, -1)
            ex.close()
            return
        }

        // Content-Disposition filename escape (quotes only)
        val filename = meta.name.replace("\"", "\\\"")

        // Headers
        ex.responseHeaders.apply {
            add("Content-Type", "application/zip")
            add("Content-Length", meta.size.toString())
            add("Content-Disposition", "attachment; filename=\"$filename\"")
        }
        ex.sendResponseHeaders(200, meta.size)

        // Body
        ex.responseBody.use { out ->
            meta.repoFile.inputStream().use { it.copyTo(out) }
        }
    }

}
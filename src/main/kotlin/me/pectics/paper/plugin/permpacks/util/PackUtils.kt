package me.pectics.paper.plugin.permpacks.util

import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.Pack
import me.pectics.paper.plugin.permpacks.data.UrlPackItem
import me.pectics.paper.plugin.permpacks.upload.UploadService
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest

fun Iterable<Pack>.toRequest(): ResourcePackRequest {
    return ResourcePackRequest.resourcePackRequest()
        .packs(flatMap { pack ->
            pack.items.map {
                when (it) {
                    is UrlPackItem -> ResourcePackInfo.resourcePackInfo()
                        .uri(it.url.toURI())
                        .apply { if (it.hash != null) hash(it.hash.value) }
                        .build()
                    is FilePackItem -> ResourcePackInfo.resourcePackInfo()
                        .uri(UploadService.urlOf(it).toURI())
                        .hash(it.hash.value)
                        .build()
                }
            }
        })
        .asResourcePackRequest()
}
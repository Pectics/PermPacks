package me.pectics.paper.plugin.permpacks.util

import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.Pack
import me.pectics.paper.plugin.permpacks.data.PackItem
import me.pectics.paper.plugin.permpacks.data.UrlPackItem
import me.pectics.paper.plugin.permpacks.upload.UploadService
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest

fun Iterable<Pack>.toRequest(): ResourcePackRequest {
    val resourcePacks = flatMap { pack ->
        pack.items.map(PackItem::toResourcePackInfo)
    }
    return ResourcePackRequest.resourcePackRequest()
        .packs(resourcePacks)
        .asResourcePackRequest()
}

private fun PackItem.toResourcePackInfo(): ResourcePackInfo = when (this) {
    is UrlPackItem -> ResourcePackInfo.resourcePackInfo()
        .uri(url.toURI())
        .apply { this@toResourcePackInfo.hash?.value?.let(::hash) }
        .build()

    is FilePackItem -> ResourcePackInfo.resourcePackInfo()
        .uri(UploadService.urlOf(this).toURI())
        .hash(hash.value)
        .build()
}

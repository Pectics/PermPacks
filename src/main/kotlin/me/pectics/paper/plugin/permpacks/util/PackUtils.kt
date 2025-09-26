package me.pectics.paper.plugin.permpacks.util

import me.pectics.paper.plugin.permpacks.data.FilePackItem
import me.pectics.paper.plugin.permpacks.data.Pack
import me.pectics.paper.plugin.permpacks.data.PackItem
import me.pectics.paper.plugin.permpacks.data.UrlPackItem
import me.pectics.paper.plugin.permpacks.upload.UploadService
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest

/**
 * 将多个 [Pack] 转换为一个 [ResourcePackRequest]
 *
 * @receiver Iterable<Pack> 要转换的资源包集合
 * @return ResourcePackRequest 转换后的资源包请求
 * @throws IllegalStateException 当包含未上传的 [FilePackItem] 时抛出
 */
fun Iterable<Pack>.toRequest(): ResourcePackRequest {
    val resourcePacks = this
        .flatMap(Pack::items)
        .distinctBy(PackItem::hash)
        .map(PackItem::toResourcePackInfo)
    return ResourcePackRequest.resourcePackRequest()
        .packs(resourcePacks)
        .asResourcePackRequest()
}

private fun PackItem.toResourcePackInfo(): ResourcePackInfo = when (this) {
    is UrlPackItem -> ResourcePackInfo.resourcePackInfo()
        .uri(url.value)
        .apply { hash?.value?.let(::hash) }
        .build()

    is FilePackItem -> {
        val url = UploadService.urlOf(this)
            ?: throw IllegalStateException("FilePackItem must be uploaded before converting to ResourcePackInfo.")
        ResourcePackInfo.resourcePackInfo()
            .uri(url.value)
            .hash(hash.value)
            .build()
    }
}

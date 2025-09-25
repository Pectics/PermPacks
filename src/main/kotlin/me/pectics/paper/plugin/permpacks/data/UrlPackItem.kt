package me.pectics.paper.plugin.permpacks.data

import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import java.net.URI

data class UrlPackItem(
    val url: URI,
    val hash: Sha1Hex?,
) : PackItem()

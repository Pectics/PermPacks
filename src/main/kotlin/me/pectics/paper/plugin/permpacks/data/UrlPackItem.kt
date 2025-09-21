package me.pectics.paper.plugin.permpacks.data

import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import java.net.URL

data class UrlPackItem(
    val url: URL,
    val hash: Sha1Hex?,
) : PackItem()

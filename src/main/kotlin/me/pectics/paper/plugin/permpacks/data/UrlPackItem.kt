package me.pectics.paper.plugin.permpacks.data

import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import me.pectics.paper.plugin.permpacks.util.SerializableURI

data class UrlPackItem(
    val url: SerializableURI,
    override val hash: Sha1Hex?,
) : PackItem()

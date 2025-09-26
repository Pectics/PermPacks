package me.pectics.paper.plugin.permpacks.data

import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex

sealed class PackItem(open val hash: Sha1Hex? = null)

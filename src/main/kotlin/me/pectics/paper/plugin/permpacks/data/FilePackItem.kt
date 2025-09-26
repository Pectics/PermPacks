package me.pectics.paper.plugin.permpacks.data

import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import java.io.File

data class FilePackItem(
    val file: File,
    override val hash: Sha1Hex,
) : PackItem()

package me.pectics.paper.plugin.permpacks.util

import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import java.io.File
import java.security.MessageDigest
import java.util.Locale.getDefault

private val SHA_1 = MessageDigest.getInstance("SHA-1")

fun File.validate() {
    if (exists() && isFile && canRead()) return
    throw IllegalArgumentException("Invalid file: $path")
}

fun File.sha1(): Sha1Hex {
    inputStream().use { fis ->
        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = fis.read(buf)
            if (read <= 0) break
            SHA_1.update(buf, 0, read)
        }
    }
    return SHA_1.digest()
        .joinToString("", transform = "%02x"::format)
        .let(Sha1Hex::of)
        .getOrThrow()
}

fun String.cap(): String {
    if (isEmpty()) return this
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }
}

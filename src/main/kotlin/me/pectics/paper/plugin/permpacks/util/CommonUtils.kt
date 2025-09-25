package me.pectics.paper.plugin.permpacks.util

import me.pectics.paper.plugin.permpacks.domain.value.Sha1Hex
import java.io.File
import java.security.MessageDigest

fun File.validate() {
    if (exists() && isFile && canRead()) return
    throw IllegalArgumentException("Invalid file: $path")
}

fun File.validated() = apply(File::validate)

fun File.sha1(): Sha1Hex {
    val sha1 = MessageDigest.getInstance("SHA-1")
    inputStream().use { fis ->
        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = fis.read(buf)
            if (read <= 0) break
            sha1.update(buf, 0, read)
        }
    }
    return sha1.digest()
        .joinToString("", transform = "%02x"::format)
        .let(Sha1Hex::of)
        .getOrThrow()
}

fun String.removePrefixIgnoreCase(prefix: String) =
    if (startsWith(prefix, ignoreCase = true))
        substring(prefix.length)
    else this

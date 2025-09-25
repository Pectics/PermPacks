package me.pectics.paper.plugin.permpacks.domain.value

import kotlinx.serialization.Serializable
import java.net.IDN
import java.text.Normalizer
import kotlin.text.Regex

@JvmInline
@Serializable
value class Host private constructor(val value: String) {

    companion object {

        private val IPV4_REGEX = Regex("""^((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.|$)){4}""")
        private val IPV6_REGEXES = listOf(
            Regex("""^([A-Za-z\d]{1,4}(:|$)){8}"""),
            Regex("""^([A-Za-z\d]{1,4}:){6}:[A-Za-z\d]{1,4}"""),
            Regex("""^([A-Za-z\d]{1,4}:){5}(:[A-Za-z\d]{1,4}){1,2}"""),
            Regex("""^([A-Za-z\d]{1,4}:){4}(:[A-Za-z\d]{1,4}){1,3}"""),
            Regex("""^([A-Za-z\d]{1,4}:){3}(:[A-Za-z\d]{1,4}){1,4}"""),
            Regex("""^([A-Za-z\d]{1,4}:){2}(:[A-Za-z\d]{1,4}){1,5}"""),
            Regex("""^[A-Za-z\d]{1,4}:((:[A-Za-z\d]{1,4}){1,6})"""),
        )
        private val LABEL_REGEXES = listOf(
            Regex("""^(?!..--)[A-Za-z\d][A-Za-z\d-]{0,61}[A-Za-z\d]$"""),
            Regex("""^xn--[A-Za-z\d-]{1,58}[A-Za-z\d]$""")
        )

        val UNINITIALIZED = Host("")

        private fun asciize(value: String) =
            try {
                IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES)
                    .lowercase()
                    .takeIf(String::isNotBlank)
            } catch (_: IllegalArgumentException) {
                null
            }

        fun of(value: String): Result<Host> {
            // ipv4 matches
            if (value.matches(IPV4_REGEX))
                return Result.success(Host(value))

            // ipv6 matches
            if (IPV6_REGEXES.any(value::matches))
                return Result.success(Host(value))

            // domain validation

            // empty check
            if (value.isBlank())
                return Result.failure(IllegalArgumentException("Invalid host syntax: $value"))

            // normalize and asciize
            val nfc = Normalizer.normalize(value, Normalizer.Form.NFC)
            val ascii = asciize(nfc)
                ?: return Result.failure(IllegalArgumentException("Invalid host syntax: $value"))

            // length check
            if (ascii.length > 253)
                return Result.failure(IllegalArgumentException("Host is too long: $value"))

            // label checks
            val labels = ascii.split('.')
            for ((i, label) in labels.withIndex()) {
                val minLength = if (i == labels.lastIndex) 2 else 1

                // length check
                if (label.length !in minLength..63)
                    return Result.failure(IllegalArgumentException("Invalid host syntax: $value"))

                // syntax check
                if (labels.any { LABEL_REGEXES.none(it::matches) })
                    return Result.failure(IllegalArgumentException("Invalid host syntax: $value"))
            }

            return Result.success(Host(ascii))
        }
    }
}
package me.pectics.paper.plugin.permpacks.domain.value

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Sha1Hex private constructor(val value: String) {

    companion object {
        fun of(hash: String?): Result<Sha1Hex> {
            if (hash == null || !hash.matches(Regex("^[a-fA-F0-9]{40}$")))
                return Result.failure(IllegalArgumentException("Invalid SHA-1 hash: $hash"))
            return Result.success(Sha1Hex(hash.lowercase()))
        }
    }

}
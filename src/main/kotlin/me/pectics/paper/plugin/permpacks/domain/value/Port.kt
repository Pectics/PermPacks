package me.pectics.paper.plugin.permpacks.domain.value

@JvmInline
value class Port private constructor(val value: UShort) {

    companion object {

        val UNINITIALIZED = Port(0u)

        fun of(port: Number): Result<Port> {
            if (port in 1..65535)
                return Result.success(Port(port.toInt().toUShort()))
            return Result.failure(IllegalArgumentException("Invalid port number: $port"))
        }
    }

}
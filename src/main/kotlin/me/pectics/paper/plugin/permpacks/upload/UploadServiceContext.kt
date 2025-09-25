package me.pectics.paper.plugin.permpacks.upload

internal class UploadServiceContext private constructor(private val map: Map<String, Any>) : Map<String, Any> by map {

    class UploadServiceContextNode(private val key: String, private val value: Any) {
        inline fun <reified T> to(): T {
            return value as? T ?: throw IllegalArgumentException(
                "Value of $key must be ${T::class.simpleName}, but got: ${value::class.simpleName}"
            )
        }
    }

    fun node(key: String): UploadServiceContextNode {
        val value = this[key]
            ?: throw IllegalArgumentException("$key is not specified.")
        return UploadServiceContextNode(key, value)
    }

    companion object {

        val EMPTY = UploadServiceContext(emptyMap())

        fun of(map: Map<String, Any>) = UploadServiceContext(map)
    }
}
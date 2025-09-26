package me.pectics.paper.plugin.permpacks.util

import kotlinx.serialization.Serializable
import me.pectics.paper.plugin.permpacks.data.serializer.URISerializer
import java.net.URI

@Serializable(with = URISerializer::class)
@JvmInline
value class SerializableURI(val value: URI) {
    override fun toString(): String = value.toString()
}

fun SerializableURI(value: String): SerializableURI = SerializableURI(URI(value))
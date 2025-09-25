package me.pectics.paper.plugin.permpacks.util

import kotlinx.serialization.Serializable
import me.pectics.paper.plugin.permpacks.data.serializer.URISerializer
import java.net.URI

@Target(AnnotationTarget.TYPEALIAS)
@Retention(AnnotationRetention.RUNTIME)
@Serializable(with = URISerializer::class)
private annotation class URLSerializability

@URLSerializability
typealias SerializableURI = URI

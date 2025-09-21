package me.pectics.paper.plugin.permpacks.util

import kotlinx.serialization.Serializable
import me.pectics.paper.plugin.permpacks.data.serializer.URLSerializer
import java.net.URL

@Target(AnnotationTarget.TYPEALIAS)
@Retention(AnnotationRetention.RUNTIME)
@Serializable(with = URLSerializer::class)
private annotation class URLSerializability

@URLSerializability
typealias SerializableURL = URL

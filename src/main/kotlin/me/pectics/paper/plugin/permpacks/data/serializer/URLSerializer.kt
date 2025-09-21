package me.pectics.paper.plugin.permpacks.data.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.net.URL

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = URL::class)
object URLSerializer : KSerializer<URL> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URL) =
        encoder.encodeString(value.toExternalForm())

    override fun deserialize(decoder: Decoder): URL =
        URI.create(decoder.decodeString()).toURL()

}

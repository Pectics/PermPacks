package me.pectics.paper.plugin.permpacks.data.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.pectics.paper.plugin.permpacks.util.SerializableURI

object URISerializer : KSerializer<SerializableURI> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SerializableURI) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): SerializableURI = SerializableURI(decoder.decodeString())
}
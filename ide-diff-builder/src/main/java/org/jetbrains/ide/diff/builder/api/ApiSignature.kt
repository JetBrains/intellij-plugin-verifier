package org.jetbrains.ide.diff.builder.api

import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer

/**
 * Base class for signatures to be recorded in API snapshots.
 */
sealed class ApiSignature

data class ClassSignature(
  val className: String
) : ApiSignature()

data class MethodSignature(
  val hostSignature: ClassSignature,
  val methodName: String,
  val methodDescriptor: String,
  val signature: String?
) : ApiSignature()

data class FieldSignature(
  val hostSignature: ClassSignature,
  val fieldName: String
) : ApiSignature()

@Serializer(forClass = ApiSignature::class)
object ApiSignatureSerializer {

  override val descriptor
    get() = PrimitiveDescriptor("ApiSignature", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, obj: ApiSignature) {
    encoder.encodeSerializableValue(String.serializer().list, obj.encodeToList())
  }

  override fun deserialize(decoder: Decoder): ApiSignature =
    decoder.decodeSerializableValue(String.serializer().list).decodeSignature()

  private fun List<String>.decodeSignature(): ApiSignature = when (first()) {
    "class" -> ClassSignature(get(1))
    "method" -> MethodSignature(
      ClassSignature(get(1)),
      get(2),
      get(3),
      get(4).takeIf { it.isNotEmpty() }
    )
    else -> FieldSignature(
      ClassSignature(get(1)),
      get(2)
    )
  }

  private fun ApiSignature.encodeToList(): List<String> = when (this) {
    is ClassSignature -> listOf(
      "class",
      className
    )
    is MethodSignature -> listOf(
      "method",
      hostSignature.className,
      methodName,
      methodDescriptor,
      signature ?: ""
    )
    is FieldSignature -> listOf(
      "field",
      hostSignature.className,
      fieldName
    )
  }
}
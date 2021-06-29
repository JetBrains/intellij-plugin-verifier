/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.ide.diff.builder.api

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
    get() = PrimitiveSerialDescriptor("ApiSignature", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: ApiSignature) {
    encoder.encodeSerializableValue(ListSerializer(String.serializer()), value.encodeToList())
  }

  override fun deserialize(decoder: Decoder): ApiSignature =
    decoder.decodeSerializableValue(ListSerializer(String.serializer())).decodeSignature()

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

/**
 * org/some/Some -> org/some/Some
 * org/some/Some$Inner -> org/some/Some
 * org/some/Some$Inner$Nested -> org/some/Some$Inner
 */
private fun getOuterClassName(className: String): String? {
  val packageName = className.substringBeforeLast("/")
  val simpleName = className.substringAfterLast("/")
  if ('$' in simpleName) {
    val outerSimpleName = simpleName.substringBeforeLast('$')
    return if (packageName.isEmpty()) {
      outerSimpleName
    } else {
      "$packageName/$outerSimpleName"
    }
  }
  return null
}

/**
 * org/some/Some -> null
 * org/some/Some$Inner -> org/some/Some
 * org/some/Some$Inner$Nested -> org/some/Some$Inner
 * org/some/Some#foo() -> org/some/Some
 * org/some/Some.x -> org/some/Some
 * org/some/Some$Inner#foo -> org/some/Some$Inner
 * org/some/Some$Inner.x -> org/some/Some$Inner
 */
val ApiSignature.containingClassSignature: ClassSignature?
  get() = when (this) {
    is ClassSignature -> getOuterClassName(className)
    is MethodSignature -> hostSignature.className
    is FieldSignature -> hostSignature.className
  }?.let { ClassSignature(it) }

/**
 * org/some/Some -> org/some/Some
 * org/some/Some$Inner -> org/some/Some
 * org/some/Some#foo() -> org/some/Some
 * org/some/Some.x -> org/some/Some
 * org/some/Some$Inner#foo -> org/some/Some
 * org/some/Some$Inner$Nested#foo -> org/some/Some
 * org/some/Some$Inner.x -> org/some/Some
 */
val ApiSignature.topLevelClassSignature: ClassSignature
  get() {
    var containingClass = when (this) {
      is ClassSignature -> this
      is MethodSignature -> this.hostSignature
      is FieldSignature -> this.hostSignature
    }
    while (containingClass.containingClassSignature != null) {
      containingClass = containingClass.containingClassSignature!!
    }
    return containingClass
  }
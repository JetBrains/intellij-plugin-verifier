package com.jetbrains.pluginverifier.utils

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonPrimitive
import com.jetbrains.pluginverifier.location.*
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.reference.FieldReference
import com.jetbrains.pluginverifier.reference.MethodReference
import com.jetbrains.pluginverifier.reference.SymbolicReference

internal val problemLocationSerializer = jsonSerializer<Location> {
  val src = it.src

  fun serializeClassPath(classPath: ClassPath): String = "${classPath.type.name}|${classPath.path}"

  fun serializeAccessFlags(accessFlags: AccessFlags): String = accessFlags.flags.toString()

  fun serializeClassLocation(src: ClassLocation): String =
      CompactJsonUtil.serialize(listOf("C", src.className, src.signature, serializeClassPath(src.classPath), serializeAccessFlags(src.accessFlags)))

  return@jsonSerializer when (src) {
    is MethodLocation -> JsonPrimitive(CompactJsonUtil.serialize(listOf("M", serializeClassLocation(src.hostClass), src.methodName, src.methodDescriptor, src.parameterNames.joinToString("|"), src.signature, serializeAccessFlags(src.accessFlags))))
    is FieldLocation -> JsonPrimitive(CompactJsonUtil.serialize(listOf("F", serializeClassLocation(src.hostClass), src.fieldName, src.fieldDescriptor, src.signature, serializeAccessFlags(src.accessFlags))))
    is ClassLocation -> JsonPrimitive(serializeClassLocation(src))
    else -> throw IllegalArgumentException("Unregistered type ${it.src.javaClass.name}: ${it.src}")
  }
}

internal val problemLocationDeserializer = jsonDeserializer {
  val parts = CompactJsonUtil.deserialize(it.json.string)

  fun deserializeClassPath(classPath: String): ClassPath {
    val cpParts = classPath.split("|")
    return ClassPath(ClassPath.Type.valueOf(cpParts[0]), cpParts[1])
  }

  fun deserializeAccessFlags(flags: String) = AccessFlags(flags.toInt())

  fun deserializeClassLocation(string: String): ClassLocation {
    val classParts = CompactJsonUtil.deserialize(string)
    return Location.fromClass(classParts[1], classParts[2], deserializeClassPath(classParts[3]), deserializeAccessFlags(classParts[4]))
  }

  return@jsonDeserializer when {
    parts[0] == "M" -> Location.fromMethod(deserializeClassLocation(parts[1]), parts[2], parts[3], parts[4].split("|"), parts[5], deserializeAccessFlags(parts[6]))
    parts[0] == "F" -> Location.fromField(deserializeClassLocation(parts[1]), parts[2], parts[3], parts[4], deserializeAccessFlags(parts[5]))
    parts[0] == "C" -> deserializeClassLocation(it.json.string)
    else -> throw IllegalArgumentException("Unknown type ${it.json.string}")
  }
}

internal val symbolicReferenceSerializer = jsonSerializer<SymbolicReference> {
  val src = it.src
  return@jsonSerializer when (src) {
    is MethodReference -> JsonPrimitive(CompactJsonUtil.serialize(listOf("M", src.hostClass.className, src.methodName, src.methodDescriptor)))
    is FieldReference -> JsonPrimitive(CompactJsonUtil.serialize(listOf("F", src.hostClass.className, src.fieldName, src.fieldDescriptor)))
    is ClassReference -> JsonPrimitive(CompactJsonUtil.serialize(listOf("C", src.className)))
    else -> throw IllegalArgumentException("Unknown type ${src.javaClass.name}: $src")
  }
}

internal val symbolicReferenceDeserializer = jsonDeserializer {
  val parts = CompactJsonUtil.deserialize(it.json.string)
  return@jsonDeserializer when {
    parts[0] == "M" -> SymbolicReference.Companion.methodOf(parts[1], parts[2], parts[3])
    parts[0] == "F" -> SymbolicReference.Companion.fieldOf(parts[1], parts[2], parts[3])
    parts[0] == "C" -> SymbolicReference.Companion.classOf(parts[1])
    else -> throw IllegalArgumentException("Unknown type ${it.json.string}")
  }
}
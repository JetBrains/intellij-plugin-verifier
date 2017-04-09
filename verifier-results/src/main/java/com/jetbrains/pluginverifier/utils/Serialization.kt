package com.jetbrains.pluginverifier.utils

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonPrimitive
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.PluginDependency
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
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

private data class DependenciesGraphCompactVersion(@SerializedName("vertices") val vertices: List<Triple<String, String, List<MissingDependency>>>,
                                                   @SerializedName("startIdx") val startIdx: Int,
                                                   @SerializedName("edges") val edges: List<Triple<Int, Int, PluginDependency>>)

internal val dependenciesGraphSerializer = jsonSerializer<DependenciesGraph> {
  val nodeToId: Map<DependencyNode, Int> = it.src.vertices.mapIndexed { i, node -> node to i }.toMap()

  val vertices = it.src.vertices.map { Triple(it.id, it.version, it.missingDependencies) }
  val startIdx = it.src.vertices.indexOf(it.src.start)
  val edges = it.src.edges.map { Triple(nodeToId[it.from]!!, nodeToId[it.to]!!, it.dependency) }
  it.context.serialize(DependenciesGraphCompactVersion(vertices, startIdx, edges))
}

internal val dependenciesGraphDeserializer = jsonDeserializer<DependenciesGraph> {
  val compact = it.context.deserialize<DependenciesGraphCompactVersion>(it.json)
  val vertices = compact.vertices.map { DependencyNode(it.first, it.second, it.third) }
  DependenciesGraph(vertices[compact.startIdx], vertices, compact.edges.map { DependencyEdge(vertices[it.first], vertices[it.second], it.third) })

}
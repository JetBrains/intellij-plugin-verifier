package com.jetbrains.pluginverifier.dependencies

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.PluginDependency

/**
 * @author Sergey Patrikeev
 */
data class MissingReason(@SerializedName("reason") val reason: String)

data class MissingPlugin(@SerializedName("pluginId") val pluginId: String)

data class DependencyNode(@SerializedName("pluginId") val pluginId: String,
                          @SerializedName("version") val version: String,
                          @SerializedName("missingDeps") val missingDependencies: Map<PluginDependency, MissingReason>)

data class DependencyEdge(@SerializedName("from") val from: DependencyNode,
                          @SerializedName("to") val to: DependencyNode,
                          @SerializedName("dependency") val dependency: PluginDependency)

data class DependenciesGraph(@SerializedName("start") val start: DependencyNode,
                             @SerializedName("vertices") val vertices: List<DependencyNode>,
                             @SerializedName("edges") val edges: List<DependencyEdge>)

//TODO: write a compact implementation
private data class DependenciesGraphCompact(@SerializedName("vertices") val vertices: List<DependencyNode>,
                                            @SerializedName("startIdx") val startIdx: Int,
                                            @SerializedName("edges") val edges: List<Triple<Int, Int, PluginDependency>>)

internal val dependenciesGraphSerializer = jsonSerializer<DependenciesGraph> {
  val nodeToId: Map<DependencyNode, Int> = it.src.vertices.mapIndexed { i, node -> node to i }.toMap()

  it.context.serialize(
      DependenciesGraphCompact(it.src.vertices, it.src.vertices.indexOf(it.src.start), it.src.edges.map { Triple(nodeToId[it.from]!!, nodeToId[it.to]!!, it.dependency) })
  )
}

internal val dependenciesGraphDeserializer = jsonDeserializer<DependenciesGraph> {
  val compact = it.context.deserialize<DependenciesGraphCompact>(it.json)
  DependenciesGraph(compact.vertices[compact.startIdx], compact.vertices, compact.edges.map { DependencyEdge(compact.vertices[it.first], compact.vertices[it.second], it.third) })
}

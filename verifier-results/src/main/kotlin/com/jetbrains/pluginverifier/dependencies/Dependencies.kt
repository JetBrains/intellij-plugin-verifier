package com.jetbrains.pluginverifier.dependencies

import com.github.salomonbrys.kotson.jsonDeserializer
import com.github.salomonbrys.kotson.jsonSerializer
import com.google.gson.annotations.SerializedName
import com.intellij.structure.domain.PluginDependency
import java.util.*

/**
 * @author Sergey Patrikeev
 */
data class MissingReason(@SerializedName("reason") val reason: String) {
  override fun toString(): String = reason
}

data class MissingPlugin(@SerializedName("pluginId") val pluginId: String) {
  override fun toString(): String = pluginId
}

data class DependencyNode(@SerializedName("pluginId") val pluginId: String,
                          @SerializedName("version") val version: String,
                          @SerializedName("missingDeps") val missingDependencies: Map<PluginDependency, MissingReason>) {
  override fun toString(): String = if (version.isNotEmpty()) "$pluginId:$version" else pluginId
}

data class DependencyEdge(@SerializedName("from") val from: DependencyNode,
                          @SerializedName("to") val to: DependencyNode,
                          @SerializedName("dependency") val dependency: PluginDependency) {
  override fun toString(): String = "$from -> $to${if (dependency.isOptional) " (optional)" else ""}"
}

data class MissingDependencyPath(@SerializedName("path") val path: List<DependencyNode>,
                                 @SerializedName("missing") val missing: PluginDependency,
                                 @SerializedName("reason") val reason: MissingReason) {
  override fun toString(): String = path.joinToString(" -> ") + " -> " + missing.id + ": $reason"
}

data class DependenciesGraph(@SerializedName("start") val start: DependencyNode,
                             @SerializedName("vertices") val vertices: List<DependencyNode>,
                             @SerializedName("edges") val edges: List<DependencyEdge>) {

  fun getMissingNonOptionalDependencies(): List<MissingDependencyPath> {
    val result = arrayListOf<MissingDependencyPath>()
    dfs(start, hashSetOf(), LinkedList(), result)
    return result
  }

  fun getMissingOptionalDependencies(): Map<PluginDependency, MissingReason> = start.missingDependencies.filterKeys { it.isOptional }

  private fun dfs(current: DependencyNode,
                  visited: MutableSet<DependencyNode>,
                  path: Deque<DependencyNode>,
                  result: MutableList<MissingDependencyPath>) {
    path.addLast(current)
    try {
      visited.add(current)
      result.addAll(current.missingDependencies.entries.filterNot { it.key.isOptional }.map { MissingDependencyPath(path.toList(), it.key, it.value) })
      edges.filter { it.from == current && !it.dependency.isOptional }.map { it.to }.forEach {
        if (it !in visited) {
          dfs(it, visited, path, result)
        }
      }
    } finally {
      path.removeLast()
    }
  }

}

private data class DependenciesGraphCompact(@SerializedName("vertices") val vertices: List<Triple<String, String, Map<PluginDependency, MissingReason>>>,
                                            @SerializedName("startIdx") val startIdx: Int,
                                            @SerializedName("edges") val edges: List<Triple<Int, Int, PluginDependency>>)

internal val dependenciesGraphSerializer = jsonSerializer<DependenciesGraph> {
  val nodeToId: Map<DependencyNode, Int> = it.src.vertices.mapIndexed { i, node -> node to i }.toMap()

  val vertices = it.src.vertices.map { Triple(it.pluginId, it.version, it.missingDependencies) }
  val startIdx = it.src.vertices.indexOf(it.src.start)
  val edges = it.src.edges.map { Triple(nodeToId[it.from]!!, nodeToId[it.to]!!, it.dependency) }
  it.context.serialize(
      DependenciesGraphCompact(vertices, startIdx, edges)
  )
}

internal val dependenciesGraphDeserializer = jsonDeserializer<DependenciesGraph> {
  val compact = it.context.deserialize<DependenciesGraphCompact>(it.json)
  val vertices = compact.vertices.map { DependencyNode(it.first, it.second, it.third) }
  DependenciesGraph(vertices[compact.startIdx], vertices, compact.edges.map { DependencyEdge(vertices[it.first], vertices[it.second], it.third) })

}

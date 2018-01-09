package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import org.jgrapht.DirectedGraph

/**
 * Utility class that converts the internal presentation of the
 * [dependencies graph] [DepVertex] to the API version [DependenciesGraph].
 */
class DepGraph2ApiGraphConverter {

  companion object {
    val UNSPECIFIED_VERSION = "<unspecified>"
  }

  fun convert(graph: DirectedGraph<DepVertex, DepEdge>, startVertex: DepVertex): DependenciesGraph {
    val startNode = graph.toDependencyNode(startVertex)!!
    val vertices = graph.vertexSet().mapNotNull { graph.toDependencyNode(it) }
    val edges = graph.edgeSet().mapNotNull { graph.toDependencyEdge(it) }
    return DependenciesGraph(startNode, vertices, edges)
  }

  private fun DirectedGraph<DepVertex, DepEdge>.toDependencyEdge(depEdge: DepEdge): DependencyEdge? {
    val from = this.toDependencyNode(getEdgeSource(depEdge)) ?: return null
    val to = this.toDependencyNode(getEdgeTarget(depEdge)) ?: return null
    return DependencyEdge(from, to, depEdge.dependency)
  }

  private fun DepEdge.toMissingDependency(): MissingDependency? {
    return with(target.dependencyResult) {
      when (this) {
        is DependencyFinder.Result.DetailsProvided -> {
          with(pluginDetailsCacheResult) {
            when (this) {
              is PluginDetailsCache.Result.Provided -> null
              is PluginDetailsCache.Result.InvalidPlugin -> MissingDependency(
                  dependency,
                  "Dependency $dependency is invalid: " + pluginErrors
                      .filter { it.level == PluginProblem.Level.ERROR }
                      .joinToString()
              )
              is PluginDetailsCache.Result.Failed -> MissingDependency(dependency, reason)
              is PluginDetailsCache.Result.FileNotFound -> MissingDependency(dependency, reason)
            }
          }
        }
        is DependencyFinder.Result.NotFound -> MissingDependency(dependency, reason)
        is DependencyFinder.Result.FoundPlugin -> null
        is DependencyFinder.Result.DefaultIdeModule -> null
      }
    }
  }

  private fun DependencyFinder.Result.getPlugin(): IdePlugin? {
    return when (this) {
      is DependencyFinder.Result.DetailsProvided -> with(pluginDetailsCacheResult) {
        when (this) {
          is PluginDetailsCache.Result.Provided -> pluginDetails.plugin
          else -> null
        }
      }
      is DependencyFinder.Result.FoundPlugin -> plugin
      is DependencyFinder.Result.NotFound -> null
      is DependencyFinder.Result.DefaultIdeModule -> null
    }
  }

  private fun DirectedGraph<DepVertex, DepEdge>.toDependencyNode(depVertex: DepVertex): DependencyNode? {
    val missingDependencies = outgoingEdgesOf(depVertex).mapNotNull { it.toMissingDependency() }
    val plugin = depVertex.dependencyResult.getPlugin()
    return plugin?.run { DependencyNode(pluginId ?: depVertex.dependencyId, pluginVersion ?: UNSPECIFIED_VERSION, missingDependencies) }
  }

}
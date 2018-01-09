package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import org.jgrapht.DirectedGraph

/**
 * Builds the dependencies graph using the [dependencyFinder].
 */
class DepGraphBuilder(private val dependencyFinder: DependencyFinder) {

  /**
   * Transitively resolves all the dependencies and adds
   * corresponding [vertices] [DepVertex] to the [graph]
   * starting from the [start].
   */
  fun buildDependenciesGraph(graph: DirectedGraph<DepVertex, DepEdge>, start: DepVertex) {
    if (!graph.containsVertex(start)) {
      graph.addVertex(start)
      val plugin = start.dependencyResult.getPlugin()
      if (plugin != null) {
        for (pluginDependency in plugin.dependencies) {
          val resolvedDependency = resolveDependency(pluginDependency, graph)
          buildDependenciesGraph(graph, resolvedDependency)
          graph.addEdge(start, resolvedDependency, DepEdge(pluginDependency))
        }
      }
    }
  }

  private fun DependencyFinder.Result.getPlugin() = when (this) {
    is DependencyFinder.Result.DetailsProvided -> when (pluginDetailsCacheResult) {
      is PluginDetailsCache.Result.Provided -> pluginDetailsCacheResult.pluginDetails.plugin
      is PluginDetailsCache.Result.InvalidPlugin -> null
      is PluginDetailsCache.Result.Failed -> null
      is PluginDetailsCache.Result.FileNotFound -> null
    }
    is DependencyFinder.Result.FoundPlugin -> plugin
    is DependencyFinder.Result.NotFound -> null
    is DependencyFinder.Result.DefaultIdeModule -> null
  }

  private fun resolveDependency(pluginDependency: PluginDependency, directedGraph: DirectedGraph<DepVertex, DepEdge>) =
      directedGraph.vertexSet()
          .find { pluginDependency.id == it.dependencyId }
          ?: DepVertex(pluginDependency.id, dependencyFinder.findPluginDependency(pluginDependency))

}
package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.plugin.PluginDetails
import org.jgrapht.DirectedGraph

class DepGraphBuilder(private val dependencyFinder: DependencyFinder) {

  fun fillDependenciesGraph(current: DepVertex, directedGraph: DirectedGraph<DepVertex, DepEdge>) {
    if (!directedGraph.containsVertex(current)) {
      directedGraph.addVertex(current)
      val plugin = current.pluginDetails.plugin
      if (plugin != null) {
        for (pluginDependency in plugin.dependencies) {
          val dependency = resolveDependency(pluginDependency, directedGraph) ?: continue
          fillDependenciesGraph(dependency, directedGraph)
          directedGraph.addEdge(current, dependency, DepEdge(pluginDependency))
        }
      }
    }
  }

  private fun resolveDependency(pluginDependency: PluginDependency, directedGraph: DirectedGraph<DepVertex, DepEdge>): DepVertex? {
    val alreadyResolved = directedGraph.vertexSet().find { pluginDependency.id == it.dependencyId }
    if (alreadyResolved == null) {
      val resolvedPluginDependency = dependencyFinder.findPluginDependency(pluginDependency)
      val pluginDetails = resolvedPluginDependency.toPluginDetails() ?: return null
      return DepVertex(pluginDependency.id, pluginDetails)
    }
    return alreadyResolved
  }

  private fun DependencyFinder.Result.toPluginDetails() = when (this) {
    is DependencyFinder.Result.FoundPluginInfo -> pluginDetailsProvider.providePluginDetails(pluginInfo)
    is DependencyFinder.Result.PluginAndDetailsProvider -> pluginDetailsProvider.provideDetailsByExistingPlugins(plugin)
    is DependencyFinder.Result.FoundOpenPluginWithoutClasses -> PluginDetails.FoundOpenPluginWithoutClasses(plugin)
    is DependencyFinder.Result.NotFound -> PluginDetails.NotFound(reason)
    is DependencyFinder.Result.DefaultIdeaModule -> null
  }

}
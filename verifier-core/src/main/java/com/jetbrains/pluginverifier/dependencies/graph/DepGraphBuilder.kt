package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyResolver
import org.jgrapht.DirectedGraph

class DepGraphBuilder(private val dependencyResolver: DependencyResolver) {

  fun fillDependenciesGraph(current: DepVertex, directedGraph: DirectedGraph<DepVertex, DepEdge>) {
    if (!directedGraph.containsVertex(current)) {
      directedGraph.addVertex(current)
      val plugin = current.pluginDetails.plugin
      if (plugin != null) {
        for (pluginDependency in plugin.dependencies) {
          val dependency = resolveDependency(pluginDependency, directedGraph)
          fillDependenciesGraph(dependency, directedGraph)
          directedGraph.addEdge(current, dependency, DepEdge(pluginDependency))
        }
      }
    }
  }

  private fun resolveDependency(pluginDependency: PluginDependency, directedGraph: DirectedGraph<DepVertex, DepEdge>): DepVertex {
    val alreadyResolved = directedGraph.vertexSet().find { pluginDependency.id == it.dependencyId }
    if (alreadyResolved == null) {
      val pluginDetails = dependencyResolver.findPluginDependency(pluginDependency).getPluginDetails()
      return DepVertex(pluginDependency.id, pluginDetails)
    }
    return alreadyResolved
  }

}
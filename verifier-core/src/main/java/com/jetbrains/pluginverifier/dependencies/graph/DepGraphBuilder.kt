package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import org.jgrapht.DirectedGraph

class DepGraphBuilder(private val dependencyFinder: DependencyFinder) {

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
      val pluginDetails = dependencyFinder.findPluginDependency(pluginDependency).getPluginDetails()
      return DepVertex(pluginDependency.id, pluginDetails)
    }
    return alreadyResolved
  }

}
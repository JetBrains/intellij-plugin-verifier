package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.plugin.PluginDetails
import org.jgrapht.DirectedGraph

class DepGraph2ApiGraphConverter {

  fun convert(graph: DirectedGraph<DepVertex, DepEdge>, startVertex: DepVertex): DependenciesGraph {
    val startNode = startVertex.toDependencyNode(graph)!!
    val vertices = graph.vertexSet().mapNotNull { it.toDependencyNode(graph) }
    val edges = graph.edgeSet().mapNotNull { it.toDependencyEdge(graph) }
    return DependenciesGraph(startNode, vertices, edges)
  }

  private fun DepEdge.toDependencyEdge(graph: DirectedGraph<DepVertex, DepEdge>): DependencyEdge? {
    val from = graph.getEdgeSource(this).toDependencyNode(graph) ?: return null
    val to = graph.getEdgeTarget(this).toDependencyNode(graph) ?: return null
    return DependencyEdge(from, to, dependency)
  }

  private fun DepEdge.toMissingDependency(): MissingDependency? {
    val pluginDetails = target.pluginDetails
    return when (pluginDetails) {
      is PluginDetails.BadPlugin -> {
        val errors = pluginDetails.pluginErrorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }
        MissingDependency(dependency, "Dependency $dependency is invalid: " + errors.joinToString())
      }
      is PluginDetails.FailedToDownload -> MissingDependency(dependency, pluginDetails.reason)
      is PluginDetails.NotFound -> MissingDependency(dependency, pluginDetails.reason)
      is PluginDetails.ByFileLock -> null
      is PluginDetails.FoundOpenPluginAndClasses -> null
      is PluginDetails.FoundOpenPluginWithoutClasses -> null
    }
  }

  private fun DepVertex.toDependencyNode(graph: DirectedGraph<DepVertex, DepEdge>): DependencyNode? {
    val missingDependencies = graph.outgoingEdgesOf(this).mapNotNull { it.toMissingDependency() }
    val plugin = pluginDetails.plugin
    return plugin?.run { DependencyNode(pluginId ?: dependencyId, pluginVersion ?: "<UNSPECIFIED>", missingDependencies) }
  }

}
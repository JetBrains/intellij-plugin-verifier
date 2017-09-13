package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import org.jgrapht.DirectedGraph

object DepGraph2ApiGraphConverter {

  private val DEFAULT_VERSION = "VERSION"

  fun convert(graph: DirectedGraph<DepVertex, DepEdge>, startVertex: DepVertex): DependenciesGraph {
    val startNode = getDependencyNodeByVertex(startVertex, graph)!!
    val vertices = graph.vertexSet().mapNotNull { getDependencyNodeByVertex(it, graph) }
    val edges = graph.edgeSet().mapNotNull { getDependencyEdgeByEdge(graph, it) }
    return DependenciesGraph(startNode, vertices, edges)
  }

  private fun getDependencyEdgeByEdge(graph: DirectedGraph<DepVertex, DepEdge>, edge: DepEdge): DependencyEdge? {
    val fromNode = getDependencyNodeByVertex(graph.getEdgeSource(edge), graph) ?: return null
    val toNode = getDependencyNodeByVertex(graph.getEdgeTarget(edge), graph) ?: return null
    return DependencyEdge(fromNode, toNode, edge.dependency)
  }

  private fun getMissingDependency(edge: DepEdge): MissingDependency? {
    val to = edge.target as DepVertex
    val resolveResult = to.resolveResult
    return when (resolveResult) {
      is DependencyResolver.Result.FoundReady -> null
      is DependencyResolver.Result.CreatedResolver -> null
      is DependencyResolver.Result.Downloaded -> null
      is DependencyResolver.Result.ProblematicDependency -> {
        val dependency = edge.dependency
        val isModule = edge.isModule
        val errors = resolveResult.pluginErrorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }
        MissingDependency(dependency, "Dependent " + (if (isModule) "module" else "plugin") + " $dependency is invalid: " + errors.joinToString())
      }
      is DependencyResolver.Result.NotFound -> MissingDependency(edge.dependency, resolveResult.reason)
      is DependencyResolver.Result.FailedToDownload -> MissingDependency(edge.dependency, resolveResult.reason)
      DependencyResolver.Result.Skip -> null
    }
  }

  private fun getDependencyNodeByVertex(vertex: DepVertex, graph: DirectedGraph<DepVertex, DepEdge>): DependencyNode? {
    val missingDependencies = graph.outgoingEdgesOf(vertex).mapNotNull { getMissingDependency(it) }
    val resolveResult = vertex.resolveResult
    return when (resolveResult) {
      is DependencyResolver.Result.FoundReady -> DependencyNode(resolveResult.plugin.pluginId ?: vertex.id, resolveResult.plugin.pluginVersion ?: DEFAULT_VERSION, missingDependencies)
      is DependencyResolver.Result.CreatedResolver -> DependencyNode(resolveResult.plugin.pluginId ?: vertex.id, resolveResult.plugin.pluginVersion ?: DEFAULT_VERSION, missingDependencies)
      is DependencyResolver.Result.Downloaded -> DependencyNode(resolveResult.plugin.pluginId ?: vertex.id, resolveResult.plugin.pluginVersion ?: DEFAULT_VERSION, missingDependencies)
      is DependencyResolver.Result.ProblematicDependency -> null
      is DependencyResolver.Result.NotFound -> null
      is DependencyResolver.Result.FailedToDownload -> null
      DependencyResolver.Result.Skip -> null
    }
  }


}
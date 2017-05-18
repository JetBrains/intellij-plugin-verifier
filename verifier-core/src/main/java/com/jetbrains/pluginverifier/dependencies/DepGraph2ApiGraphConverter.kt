package com.jetbrains.pluginverifier.dependencies

import org.jgrapht.DirectedGraph

object DepGraph2ApiGraphConverter {

  fun convert(graph: DirectedGraph<DepVertex, DepEdge>, startVertex: DepVertex): DependenciesGraph =
      DependenciesGraph(
          getDependencyNodeByVertex(startVertex),
          graph.vertexSet().map { getDependencyNodeByVertex(it) },
          graph.edgeSet().map { getDependencyEdgeByEdge(graph, it) }
      )

  private fun getDependencyEdgeByEdge(graph: DirectedGraph<DepVertex, DepEdge>, edge: DepEdge): DependencyEdge =
      DependencyEdge(
          getDependencyNodeByVertex(graph.getEdgeSource(edge)),
          getDependencyNodeByVertex(graph.getEdgeTarget(edge)),
          edge.dependency
      )

  private fun getDependencyNodeByVertex(vertex: DepVertex): DependencyNode =
      DependencyNode(vertex.creationOk.success.plugin.pluginId ?: "", vertex.creationOk.success.plugin.pluginVersion ?: "", vertex.missingDependencies)


}
package com.jetbrains.pluginverifier.dependencies

import org.jgrapht.DirectedGraph
import org.jgrapht.alg.cycle.JohnsonSimpleCycles
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

data class DependenciesGraphCycleFinder(val dependenciesGraph: DependenciesGraph) {

  fun findAllCycles(): List<List<DependencyNode>> {
    val graph: DirectedGraph<DependencyNode, DefaultEdge> = DefaultDirectedGraph(DefaultEdge::class.java)
    dependenciesGraph.vertices.forEach { graph.addVertex(it) }
    dependenciesGraph.edges.forEach { graph.addEdge(it.from, it.to) }
    return JohnsonSimpleCycles(graph).findSimpleCycles()
  }

}
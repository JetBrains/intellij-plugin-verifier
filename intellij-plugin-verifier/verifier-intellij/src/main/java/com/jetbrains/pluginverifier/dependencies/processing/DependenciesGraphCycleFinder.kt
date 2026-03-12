/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.processing

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector
import org.jgrapht.alg.cycle.JohnsonSimpleCycles
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.util.*

data class DependenciesGraphCycleFinder(val dependenciesGraph: DependenciesGraph) {

  /**
   * Returns all cycles in the [dependenciesGraph] that contain the [dependenciesGraph.verifiedPlugin].
   */
  fun findAllCyclesWithVerifiedPlugin(): List<List<DependencyNode>> {
    val graph = DefaultDirectedGraph<DependencyNode, DefaultEdge>(DefaultEdge::class.java)
    dependenciesGraph.vertices.forEach { graph.addVertex(it) }
    dependenciesGraph.edges.forEach { graph.addEdge(it.from, it.to) }

    val verifiedPluginVertex = dependenciesGraph.verifiedPlugin
    if (!graph.containsVertex(verifiedPluginVertex)) {
      return emptyList()
    }

    val stronglyConnectedVertices = GabowStrongConnectivityInspector(graph)
      .stronglyConnectedSets()
      .find { verifiedPluginVertex in it } ?: return emptyList()

    // Less than 2 strongly connected vertices indicate no cycles.
    // Usually, it is a disconnected graph.
    if (stronglyConnectedVertices.size < 2) {
      return emptyList()
    }

    val stronglyConnectedVerticesSubgraph = AsSubgraph(graph, stronglyConnectedVertices)
    return JohnsonSimpleCycles(stronglyConnectedVerticesSubgraph).findSimpleCycles().filter {
      verifiedPluginVertex in it
    }.map {
      it.rotateToFront(verifiedPluginVertex)
    }
  }

  private fun <T> List<T>.rotateToFront(target: T): List<T> {
    val result = this.toMutableList()
    val index = indexOf(target)
    require(index != -1) { "Target element not found: $target" }

    Collections.rotate(result, -index)
    return result
  }
}

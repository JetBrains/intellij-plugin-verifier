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
    // 'Any' edge corresponds to `java.lang.Object` as per https://jgrapht.org/guide/VertexAndEdgeTypes#anonymous-edges
    val graph = DefaultDirectedGraph<DependencyNode, Any>(Any::class.java)
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

  /**
   * Dumps the dependency graph as two CSV files for use with graph visualizers:
   * - `graph-<timestamp>-nodes.csv`: `id,label` — one row per node
   * - `graph-<timestamp>-links.csv`: `source,target` — one row per edge
   */
  fun dumpGraph(graph: Graph<DependencyNode, DefaultEdge>) {
    val timestamp = LocalDateTime.now()
    Files.newBufferedWriter(Path("graph-$timestamp-nodes.csv")).use { writer ->
      writer.appendLine("id,label")
      for (node in graph.vertexSet()) {
        writer.appendLine("${node.graphId()},$node")
      }
    }
    Files.newBufferedWriter(Path("graph-$timestamp-links.csv")).use { writer ->
      writer.appendLine("source,target")
      for (edge in graph.edgeSet()) {
        writer.appendLine("${graph.getEdgeSource(edge).graphId()},${graph.getEdgeTarget(edge).graphId()}")
      }
    }
  }
}

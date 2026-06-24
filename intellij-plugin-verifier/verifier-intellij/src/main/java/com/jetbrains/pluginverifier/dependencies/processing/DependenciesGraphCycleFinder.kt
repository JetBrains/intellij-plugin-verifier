/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.processing

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.nio.file.Files
import java.time.LocalDateTime
import kotlin.io.path.Path

private const val DUMP_GRAPH_PROPERTY = "pluginverifier.dumpDependencyGraph"

data class DependenciesGraphCycleFinder(val dependenciesGraph: DependenciesGraph) {

  /**
   * Checks for cycles in the [dependenciesGraph] that involve the verified plugin.
   * If one is found, [fn] is invoked with it.
   */
  fun checkForCycle(fn: (List<DependencyNode>) -> Unit) {
    val graph = DefaultDirectedGraph<DependencyNode, DefaultEdge>(DefaultEdge::class.java)

    val normalizer = DependencyNodeNormalizer()
    dependenciesGraph.vertices.forEach { graph.addVertex(normalizer.normalize(it)) }
    dependenciesGraph.edges.forEach { graph.addEdge(normalizer.normalize(it.from), normalizer.normalize(it.to)) }

    if (System.getProperty(DUMP_GRAPH_PROPERTY) != null) dumpGraph(graph)

    GabowStrongConnectivityInspector(graph)
      .stronglyConnectedSets()
      .firstOrNull { scc -> scc.size > 1 && dependenciesGraph.verifiedPlugin in scc }
      ?.let { scc ->
        findCycleThrough(AsSubgraph(graph, scc), dependenciesGraph.verifiedPlugin)?.let(fn)
      }
  }

  /**
   * BFS from [start] within [graph] to find the shortest cycle passing through [start].
   * Returns the cycle as an ordered list beginning with [start], or null if none exists.
   */
  private fun findCycleThrough(
    graph: Graph<DependencyNode, DefaultEdge>,
    start: DependencyNode
  ): List<DependencyNode>? {
    val parent = HashMap<DependencyNode, DependencyNode>()
    val queue = ArrayDeque<DependencyNode>()

    for (edge in graph.outgoingEdgesOf(start)) {
      val neighbor = graph.getEdgeTarget(edge)
      if (neighbor == start) return listOf(start)
      if (neighbor !in parent) {
        parent[neighbor] = start
        queue.add(neighbor)
      }
    }

    while (queue.isNotEmpty()) {
      val node = queue.removeFirst()
      for (edge in graph.outgoingEdgesOf(node)) {
        val next = graph.getEdgeTarget(edge)
        if (next == start) return reconstructCycle(parent, start, node)
        if (next !in parent) {
          parent[next] = node
          queue.add(next)
        }
      }
    }
    return null
  }

  private fun reconstructCycle(
    parent: Map<DependencyNode, DependencyNode>,
    start: DependencyNode,
    end: DependencyNode
  ): List<DependencyNode> {
    val segment = mutableListOf<DependencyNode>()
    var current = end
    while (current != start) {
      segment.add(current)
      current = parent[current]!!
    }
    segment.reverse()
    return listOf(start) + segment
  }

  /**
   * Interns [DependencyNode] instances by structural equality ([equals]/[hashCode]),
   * ensuring that the same logical node maps to a single canonical instance within a
   * single graph build. This is required because jgrapht requires edge endpoints to be
   * the same instances registered as vertices.
   */
  class DependencyNodeNormalizer {
    private val cache = HashMap<DependencyNode, DependencyNode>()

    fun normalize(node: DependencyNode): DependencyNode =
      cache.getOrPut(node) { node }
  }

  /**
   * Dumps the dependency graph as two CSV files for use with graph visualizers:
   * - `graph-<timestamp>-nodes.csv`: `id,label` — one row per node
   * - `graph-<timestamp>-links.csv`: `source,target` — one row per edge
   *
   * Enable by setting the `-Dpluginverifier.dumpDependencyGraph` JVM system property.
   */
  private fun dumpGraph(graph: Graph<DependencyNode, DefaultEdge>) {
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

  /** Returns a 10-digit hex hash, used as the CSV node ID. */
  private fun DependencyNode.graphId(): String =
    Integer.toHexString(this.hashCode()).padStart(10, '0')
}

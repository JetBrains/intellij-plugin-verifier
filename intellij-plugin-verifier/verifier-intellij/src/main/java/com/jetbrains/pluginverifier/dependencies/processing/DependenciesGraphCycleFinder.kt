/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.processing

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import org.jgrapht.Graph
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector
import org.jgrapht.alg.cycle.JohnsonSimpleCycles
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.nio.file.Files
import java.time.LocalDateTime
import java.util.Collections
import kotlin.io.path.Path

private const val DUMP_GRAPH_PROPERTY = "pluginverifier.dumpDependencyGraph"

data class DependenciesGraphCycleFinder(val dependenciesGraph: DependenciesGraph) {

  private class CycleFound(val cycle: List<DependencyNode>) : Throwable(null, null, true, false)

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
      .filter { scc -> scc.size > 1 && dependenciesGraph.verifiedPlugin in scc }
      .forEach { scc ->
        try {
          JohnsonSimpleCycles(AsSubgraph(graph, scc))
            .findSimpleCycles { cycle ->
              if (dependenciesGraph.verifiedPlugin in cycle) throw CycleFound(cycle)
            }
        } catch (e: CycleFound) {
          fn(e.cycle.rotateToFront(dependenciesGraph.verifiedPlugin))
          return
        }
      }
  }

  private fun <T> List<T>.rotateToFront(target: T): List<T> {
    val result = this.toMutableList()
    val index = indexOf(target)
    require(index != -1) { "Target element not found: $target" }

    Collections.rotate(result, -index)
    return result
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

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.dependencies.processing.DependenciesGraphCycleFinder
import com.jetbrains.pluginverifier.dependencies.processing.DependenciesGraphWalker
import java.util.*
import kotlin.collections.ArrayList

/**
 * Graph of [plugin dependencies] [com.jetbrains.plugin.structure.intellij.plugin.PluginDependency]
 * built for the plugin verification.
 *
 * The graph is stored as a set of [vertices] and [edges]. The starting vertex is [verifiedPlugin].
 */
data class DependenciesGraph(
    val verifiedPlugin: DependencyNode,
    val vertices: List<DependencyNode>,
    val edges: List<DependencyEdge>,
    val missingDependencies: Map<DependencyNode, Set<MissingDependency>>
) {

  /**
   * Returns all missing dependencies required by the verified plugin directly.
   */
  fun getDirectMissingDependencies(): Set<MissingDependency> =
      missingDependencies.getOrDefault(verifiedPlugin, emptySet())

  /**
   * Returns all edges starting at the specified node.
   */
  fun getEdgesFrom(dependencyNode: DependencyNode): List<DependencyEdge> =
      edges.filter { it.from == dependencyNode }

  /**
   * Returns all cycles in this graph.
   * The dependencies cycles are harmful and should be fixed.
   */
  fun getAllCycles(): List<List<DependencyNode>> =
      DependenciesGraphCycleFinder(this)
          .findAllCycles()
          .filter { verifiedPlugin in it }
          .map { it.reversed() }

  /**
   * Finds all the transitive dependencies starting at the [verifiedPlugin]
   * and returns all the paths ending in [MissingDependency]s.
   */
  fun getMissingDependencyPaths(): List<MissingDependencyPath> {
    val breadCrumbs: Deque<DependencyNode> = LinkedList()
    val result: MutableList<MissingDependencyPath> = arrayListOf()
    val onVisit: (DependencyNode) -> Unit = {
      breadCrumbs.addLast(it)
      val copiedPath = ArrayList(breadCrumbs)
      val elements = missingDependencies.getOrDefault(it, emptySet()).map { MissingDependencyPath(copiedPath, it) }
      result.addAll(elements)
    }
    val onExit: (DependencyNode) -> Unit = { breadCrumbs.removeLast() }
    DependenciesGraphWalker(this, onVisit, onExit).walk(verifiedPlugin)
    return result
  }

  override fun toString() = DependenciesGraphPrettyPrinter(this).prettyPresentation()

}

/**
 * Represents an edge in the [DependenciesGraph],
 * which is a [dependency] of the plugin [from] on the plugin [to].
 */
data class DependencyEdge(
    val from: DependencyNode,
    val to: DependencyNode,
    val dependency: PluginDependency
) {
  override fun toString() = if (dependency.isOptional) "$from ---optional---> $to" else "$from ---> $to"
}

/**
 * Represents a node in the [DependenciesGraph].
 *
 * The node is a plugin [pluginId] and [version].
 */
data class DependencyNode(val pluginId: String, val version: String) {
  override fun toString() = "$pluginId:$version"
}

/**
 * Represents a [dependency] of the [verified plugin] [DependenciesGraph.verifiedPlugin]
 * that was not resolved due to [missingReason].
 */
data class MissingDependency(val dependency: PluginDependency, val missingReason: String) {
  override fun toString() = "$dependency: $missingReason"
}

/**
 * Contains a path of [DependencyNode]s in the [DependenciesGraph]
 * starting at the [DependenciesGraph.verifiedPlugin] and ending in some
 * [missing] [missingDependency] dependency.
 */
data class MissingDependencyPath(
    val path: List<DependencyNode>,
    val missingDependency: MissingDependency
) {
  override fun toString() = path.joinToString(" ---X--> ") + " ---X--> " + missingDependency
}
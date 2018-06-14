package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.dependencies.processing.DependenciesGraphCycleFinder
import com.jetbrains.pluginverifier.dependencies.processing.DependenciesGraphWalker
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Graph of [plugin dependencies] [com.jetbrains.plugin.structure.intellij.plugin.PluginDependency]
 * built for the plugin verification.
 *
 * The graph is stored as a set of [vertices] and [edges] starting at the [verifiedPlugin].
 *
 * The [nodes] [DependencyNode] contain additional data
 * on [missing] [MissingDependency] dependencies.
 */
data class DependenciesGraph(
    val verifiedPlugin: DependencyNode,
    val vertices: List<DependencyNode>,
    val edges: List<DependencyEdge>
) : Serializable {

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
      val elements = it.missingDependencies.map { MissingDependencyPath(copiedPath, it) }
      result.addAll(elements)
    }
    val onExit: (DependencyNode) -> Unit = { breadCrumbs.removeLast() }
    DependenciesGraphWalker(this, onVisit, onExit).walk(verifiedPlugin)
    return result
  }

  override fun toString() = DependenciesGraphPrettyPrinter(this).prettyPresentation()

  companion object {
    private const val serialVersionUID = 0L
  }

}

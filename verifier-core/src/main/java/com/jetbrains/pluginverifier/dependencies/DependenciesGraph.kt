package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.dependencies.processing.DependenciesGraphCycleFinder
import com.jetbrains.pluginverifier.dependencies.processing.DependenciesGraphWalker
import java.util.*
import kotlin.collections.ArrayList

data class DependenciesGraph(val start: DependencyNode,
                             val vertices: List<DependencyNode>,
                             val edges: List<DependencyEdge>) {

  fun getCycles(): List<List<DependencyNode>> = DependenciesGraphCycleFinder(this).findAllCycles().map { it.reversed() }

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
    DependenciesGraphWalker(this, onVisit, onExit).walk(start)
    return result
  }

  override fun toString(): String = DependenciesGraphPrettyPrinter(this).prettyPresentation()

}

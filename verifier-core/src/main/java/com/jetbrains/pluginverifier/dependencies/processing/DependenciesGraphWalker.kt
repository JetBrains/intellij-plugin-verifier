package com.jetbrains.pluginverifier.dependencies.processing

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode

data class DependenciesGraphWalker(val graph: DependenciesGraph,
                                   val onVisit: (DependencyNode) -> Unit,
                                   val onExit: (DependencyNode) -> Unit) {

  private val visited: MutableSet<DependencyNode> = hashSetOf()

  fun walk(current: DependencyNode): DependenciesGraphWalker {
    visited.add(current)
    try {
      onVisit(current)
      graph.edges.filter { it.from == current }.map { it.to }.forEach { to ->
        if (to !in visited) {
          walk(to)
        }
      }
      return this
    } finally {
      onExit(current)
    }
  }

}
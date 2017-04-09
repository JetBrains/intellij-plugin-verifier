package com.jetbrains.pluginverifier.dependencies

data class DependenciesGraphWalker(val graph: DependenciesGraph,
                                   val onVisit: (DependencyNode) -> Unit,
                                   val onExit: (DependencyNode) -> Unit) {

  private val visited: MutableSet<DependencyNode> = hashSetOf()

  fun walk(current: DependencyNode): DependenciesGraphWalker {
    visited.add(current)
    try {
      onVisit.invoke(current)
      graph.edges.filter { it.from == current }.map { it.to }.forEach {
        if (it !in visited) {
          walk(it)
        }
      }
      return this
    } finally {
      onExit.invoke(current)
    }
  }

}
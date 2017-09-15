package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import java.util.*
import kotlin.collections.ArrayList

//todo: make missingReason a separate class with more information.
data class MissingDependency(val dependency: PluginDependency,
                             val missingReason: String) {
  override fun toString(): String = "$dependency: $missingReason"
}

data class DependencyNode(val id: String,
                          val version: String,
                          val missingDependencies: List<MissingDependency>) {
  override fun toString(): String = "$id:$version"
}

data class DependencyEdge(val from: DependencyNode,
                          val to: DependencyNode,
                          val dependency: PluginDependency) {
  override fun toString(): String = if (dependency.isOptional) "$from ---optional---> $to" else "$from ---> $to"
}

data class MissingDependencyPath(val path: List<DependencyNode>,
                                 val missingDependency: MissingDependency) {
  override fun toString(): String = path.joinToString(" ---X--> ") + " ---X--> " + missingDependency
}

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

  override fun toString(): String = buildString {
    append("Start: $start; Vertices: ${vertices.size}; Edges: ${edges.size};")
    DependenciesGraphWalker(this@DependenciesGraph, { node ->
      val edgesFromNode = edges.filter { node == it.from }
      if (edgesFromNode.isNotEmpty()) {
        appendln()
        append("From $node to [${edgesFromNode.map { edge -> edge.to.toString() + if (edge.dependency.isOptional) " (optional)" else "" }.joinToString()}]")
      }
      if (node.missingDependencies.isNotEmpty()) {
        appendln()
        append("Missing dependencies of $node: " + node.missingDependencies.joinToString())
      }
    }, {}).walk(start)
  }


}

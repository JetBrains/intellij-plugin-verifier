package com.jetbrains.pluginverifier.dependencies

import com.google.gson.annotations.SerializedName
import com.intellij.structure.plugin.PluginDependency
import java.util.*
import kotlin.collections.ArrayList

data class MissingDependency(@SerializedName("dependency") val dependency: PluginDependency,
                             @SerializedName("isModule") val isModule: Boolean,
                             @SerializedName("missingReason") val missingReason: String) {
  override fun toString(): String = "${if (isModule) "module" else "plugin"} $dependency"
}

data class DependencyNode(@SerializedName("id") val id: String,
                          @SerializedName("version") val version: String,
                          @SerializedName("missingDeps") val missingDependencies: List<MissingDependency>) {
  override fun toString(): String = if (version.isNotEmpty()) "$id:$version" else id
}

data class DependencyEdge(@SerializedName("from") val from: DependencyNode,
                          @SerializedName("to") val to: DependencyNode,
                          @SerializedName("dependency") val dependency: PluginDependency) {
  override fun toString(): String = "$from -> $to" + (if (dependency.isOptional) " (optional)" else "")
}

data class MissingDependencyPath(@SerializedName("path") val path: List<DependencyNode>,
                                 @SerializedName("missingDependency") val missingDependency: MissingDependency) {
  override fun toString(): String = path.joinToString(" -> ") + " -> " + missingDependency + ": ${missingDependency.missingReason}"
}

data class DependenciesGraph(@SerializedName("start") val start: DependencyNode,
                             @SerializedName("vertices") val vertices: List<DependencyNode>,
                             @SerializedName("edges") val edges: List<DependencyEdge>) {

  fun getCycles(): List<List<DependencyNode>> = DependenciesGraphCycleFinder(this).findAllCycles().map { it.reversed() }

  fun getMissingDependencies(): List<MissingDependencyPath> {
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

}

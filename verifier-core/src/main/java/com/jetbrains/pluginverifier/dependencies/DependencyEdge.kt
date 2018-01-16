package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

/**
 * Represents an edge in the [DependenciesGraph]
 * a plugin [from] to a plugin [to] by [dependency].
 */
data class DependencyEdge(val from: DependencyNode,
                          val to: DependencyNode,
                          val dependency: PluginDependency) {
  override fun toString() = if (dependency.isOptional) "$from ---optional---> $to" else "$from ---> $to"
}
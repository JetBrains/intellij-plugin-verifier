package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

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
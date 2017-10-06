package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

data class DependencyEdge(val from: DependencyNode,
                          val to: DependencyNode,
                          val dependency: PluginDependency) {
  override fun toString(): String = if (dependency.isOptional) "$from ---optional---> $to" else "$from ---> $to"
}
package com.jetbrains.pluginverifier.utils.dependencies

import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency

/**
 * @author Sergey Patrikeev
 */
class PluginDependenciesNode(val plugin: Plugin,
                             /**
                              * The set of existing [PluginDependenciesNode]s reachable from `this` node.
                              */
                             val edges: Set<PluginDependenciesNode>,
                             /**
                              * All plugins reachable from [plugin].
                              */
                             val transitiveDependencies: Set<Plugin>,
                             /**
                              * Missing dependency -> reason why it is missing.
                              */
                             val missingDependencies: Map<PluginDependency, MissingReason>) {


  override fun toString(): String {
    return plugin.pluginId
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false

    val that = other as PluginDependenciesNode?

    return plugin.pluginFile == that!!.plugin.pluginFile
  }

  override fun hashCode(): Int {
    return plugin.pluginFile.hashCode()
  }
}

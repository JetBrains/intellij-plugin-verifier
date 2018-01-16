package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

/**
 * Represents a [dependency] of the [verified plugin] [DependenciesGraph.verifiedPlugin]
 * that was not resolved due to [missingReason].
 */
data class MissingDependency(val dependency: PluginDependency,
                             val missingReason: String) {
  override fun toString(): String = "$dependency: $missingReason"
}
package com.jetbrains.pluginverifier.results.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependencyNode

/**
 * The [verified plugin] [com.jetbrains.pluginverifier.results.VerificationResult.plugin] belongs
 * to a dependency cycle. It is a bad practice as IDE may refuse to load such plugin.
 */
data class DependenciesCycleWarning(val cycle: List<DependencyNode>) : PluginProblem() {
  override val level = Level.WARNING

  override val message
    get() = "The plugin is on a dependencies cycle: " + cycle.joinToString(separator = " -> ") + " -> " + cycle[0]
}
package com.jetbrains.pluginverifier.results.warnings

import com.jetbrains.plugin.structure.base.plugin.PluginProblem

/**
 * The [verified plugin] [com.jetbrains.pluginverifier.results.VerificationResult.plugin] belongs
 * to a dependency [cycle] [cyclePresentation]. It is a bad practice as IDE may refuse to load
 * such plugin.
 *
 * The dependencies' versions that constitute the cycle can be obtained from the
 * [dependencies] [com.jetbrains.pluginverifier.results.VerificationResult.dependenciesGraph] graph.
 */
data class DependenciesCycleWarning(val cyclePresentation: String) : PluginProblem() {
  override val level = Level.WARNING

  override val message
    get() = "The plugin is on a dependencies cycle: $cyclePresentation"
}
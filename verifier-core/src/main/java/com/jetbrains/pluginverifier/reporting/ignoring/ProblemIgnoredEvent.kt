package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

data class ProblemIgnoredEvent(
    val plugin: PluginInfo,
    val ideVersion: IdeVersion,
    val problem: CompatibilityProblem,
    val reason: String
) {
  override fun toString(): String = "Problem of the plugin $plugin in $ideVersion was ignored: $reason:\n    $problem"
}
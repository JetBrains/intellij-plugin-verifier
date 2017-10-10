package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.results.problems.Problem

/**
 * @author Sergey Patrikeev
 */
data class ProblemIgnoredEvent(val plugin: PluginCoordinate, val ideVersion: IdeVersion, val problem: Problem, val reason: String) {
  override fun toString(): String = "Problem of the plugin $plugin in $ideVersion was ignored: $reason:\n    $problem"
}
package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

data class ProblemIgnoredEvent(
    val plugin: PluginInfo,
    val verificationTarget: VerificationTarget,
    val problem: CompatibilityProblem,
    val reason: String
) {
  override fun toString(): String = "Problem of the plugin $plugin against $verificationTarget has been ignored: $reason:\n    $problem"
}
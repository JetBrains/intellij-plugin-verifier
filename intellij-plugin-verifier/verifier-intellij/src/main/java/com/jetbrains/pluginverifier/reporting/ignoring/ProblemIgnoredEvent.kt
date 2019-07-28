package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

data class ProblemIgnoredEvent(
    val plugin: PluginInfo,
    val verificationTarget: PluginVerificationTarget,
    val problem: CompatibilityProblem,
    val reason: String
) {
  override fun toString() = "Problem of $plugin against $verificationTarget has been ignored: $reason:\n    $problem"
}
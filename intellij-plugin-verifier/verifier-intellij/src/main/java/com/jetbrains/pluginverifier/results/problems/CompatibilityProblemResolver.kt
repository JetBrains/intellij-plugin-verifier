package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.warnings.CompatibilityWarning

interface CompatibilityProblemResolver {
  fun resolveCompatibilityProblems(context: PluginVerificationContext): List<CompatibilityProblem>

  fun resolveCompatibilityWarnings(context: PluginVerificationContext): List<CompatibilityWarning>
}
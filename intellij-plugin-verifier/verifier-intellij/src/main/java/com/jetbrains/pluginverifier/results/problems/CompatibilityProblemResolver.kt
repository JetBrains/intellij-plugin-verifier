package com.jetbrains.pluginverifier.results.problems

import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext

interface CompatibilityProblemResolver {
  fun resolveCompatibilityProblems(context: PluginVerificationContext): List<CompatibilityProblem>
}
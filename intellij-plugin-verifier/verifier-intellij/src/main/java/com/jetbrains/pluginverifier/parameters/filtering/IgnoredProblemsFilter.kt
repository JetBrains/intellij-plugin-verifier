package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * [ProblemsFilter] that ignores problems specified in [ignoreConditions].
 */
class IgnoredProblemsFilter(val ignoreConditions: List<IgnoreCondition>) : ProblemsFilter {

  override fun shouldReportProblem(
      problem: CompatibilityProblem,
      context: VerificationContext
  ): ProblemsFilter.Result {
    if (context !is PluginVerificationContext) {
      return ProblemsFilter.Result.Report
    }
    val currentId = context.idePlugin.pluginId
    val currentVersion = context.idePlugin.pluginVersion

    for ((pluginId, version, pattern) in ignoreConditions) {
      if (pluginId == null || pluginId == currentId) {
        if (version == null || version == currentVersion) {
          if (problem.shortDescription.matches(pattern)) {
            return ProblemsFilter.Result.Ignore("the problem is ignored by RegExp pattern: \"$pattern\"")
          }
        }
      }
    }
    return ProblemsFilter.Result.Report
  }

}
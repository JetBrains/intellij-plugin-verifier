package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext

/**
 * [ProblemsFilter] that ignores problems specified in [ignoreConditions].
 */
class IgnoredProblemsFilter(val ignoreConditions: List<IgnoreCondition>) : ProblemsFilter {

  override fun shouldReportProblem(
      problem: CompatibilityProblem,
      context: PluginVerificationContext
  ): ProblemsFilter.Result {
    val currentId = context.plugin.pluginId
    val currentVersion = context.plugin.version

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
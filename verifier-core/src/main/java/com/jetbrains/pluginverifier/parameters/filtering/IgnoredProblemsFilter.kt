package com.jetbrains.pluginverifier.parameters.filtering

import com.google.common.collect.Multimap
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

class IgnoredProblemsFilter(private val problemsToIgnore: Multimap<PluginIdAndVersion, Regex>) : ProblemsFilter {

  override fun shouldReportProblem(problem: CompatibilityProblem, verificationContext: VerificationContext): ProblemsFilter.Result {
    val xmlId = verificationContext.plugin.pluginId
    val version = verificationContext.plugin.pluginVersion
    for ((pluginIdAndVersion, ignoredPattern) in problemsToIgnore.entries()) {
      val ignoreXmlId = pluginIdAndVersion.pluginId
      val ignoreVersion = pluginIdAndVersion.version

      if (xmlId == ignoreXmlId) {
        if (ignoreVersion.isEmpty() || version == ignoreVersion) {
          if (problem.shortDescription.matches(ignoredPattern)) {
            return ProblemsFilter.Result.Ignore("ignoring pattern matches - $ignoredPattern")
          }
        }
      }
    }
    return ProblemsFilter.Result.Report
  }

}
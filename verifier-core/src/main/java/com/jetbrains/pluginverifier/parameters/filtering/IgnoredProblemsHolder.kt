package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

/**
 * Holds the [CompatibilityProblem]s that have been ignored by [ProblemsFilter].
 * Those problems will be logged using the [pluginVerificationReportage].
 */
class IgnoredProblemsHolder(private val pluginVerificationReportage: PluginVerificationReportage) {

  val ignoredProblems = hashSetOf<CompatibilityProblem>()

  fun isIgnored(problem: CompatibilityProblem): Boolean =
      problem in ignoredProblems

  fun registerIgnoredProblem(problem: CompatibilityProblem,
                             ignoreDecisions: List<ProblemsFilter.Result.Ignore>) {
    if (problem !in ignoredProblems) {
      ignoredProblems.add(problem)
      ignoreDecisions.forEach { pluginVerificationReportage.logProblemIgnored(problem, it.reason) }
    }
  }
}
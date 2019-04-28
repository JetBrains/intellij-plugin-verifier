package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext

/**
 * [ProblemsFilter] that yield only IDEA related problems.
 */
class IdeaOnlyProblemsFilter : ProblemsFilter {

  private val androidProblemsFilter = AndroidProblemsFilter()

  override fun shouldReportProblem(
      problem: CompatibilityProblem,
      context: PluginVerificationContext
  ): ProblemsFilter.Result {
    val result = androidProblemsFilter.shouldReportProblem(problem, context)
    return when (result) {
      ProblemsFilter.Result.Report -> ProblemsFilter.Result.Ignore("the problem belongs to Android subsystem")
      is ProblemsFilter.Result.Ignore -> ProblemsFilter.Result.Report
    }
  }
}
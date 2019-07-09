package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * [ProblemsFilter] that yield only IDEA related problems.
 */
class IdeaOnlyProblemsFilter : ProblemsFilter {

  private val androidProblemsFilter = AndroidProblemsFilter()

  override fun shouldReportProblem(
      problem: CompatibilityProblem,
      context: VerificationContext
  ): ProblemsFilter.Result {
    return when (androidProblemsFilter.shouldReportProblem(problem, context)) {
      ProblemsFilter.Result.Report -> ProblemsFilter.Result.Ignore("the problem belongs to Android subsystem")
      is ProblemsFilter.Result.Ignore -> ProblemsFilter.Result.Report
    }
  }
}
package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Ignores compatibility problems related to
 * IDEA code, and leaves only Android-related problems.
 */
class IdeaProblemsFilter : ProblemsFilter {

  private val androidFilter = AndroidProblemsFilter()

  override fun shouldReportProblem(problem: CompatibilityProblem, verificationContext: VerificationContext): ProblemsFilter.Result {
    val result = androidFilter.shouldReportProblem(problem, verificationContext)
    return when (result) {
      ProblemsFilter.Result.Report -> ProblemsFilter.Result.Ignore("the problem belongs to IDEA source code, not to Android sources authored by Google team")
      is ProblemsFilter.Result.Ignore -> ProblemsFilter.Result.Report
    }
  }
}
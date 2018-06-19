package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.analysis.getClassInducingProblem
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * [ProblemsFilter] that yields only Android related problems.
 */
class AndroidProblemsFilter : ProblemsFilter {

  private companion object {

    val androidPackages = listOf("com.android", "org/jetbrains.android").map { it.replace('.', '/') }

    fun String.belongsToAndroid() = androidPackages.any { startsWith("$it/") }

  }

  override fun shouldReportProblem(
      problem: CompatibilityProblem,
      verificationContext: VerificationContext
  ): ProblemsFilter.Result {
    val inducingClass = problem.getClassInducingProblem()
    val report = inducingClass?.belongsToAndroid() ?: false
    return if (report) {
      ProblemsFilter.Result.Report
    } else {
      ProblemsFilter.Result.Ignore("the problem doesn't belong to Android subsystem")
    }
  }
}
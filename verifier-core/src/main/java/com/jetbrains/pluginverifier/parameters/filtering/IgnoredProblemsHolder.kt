package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.reporting.verification.PluginVerificationReportage
import com.jetbrains.pluginverifier.results.problems.Problem

class IgnoredProblemsHolder(private val pluginVerificationReportage: PluginVerificationReportage) {

  val ignoredProblems = hashSetOf<Problem>()

  fun isIgnored(problem: Problem): Boolean =
      problem in ignoredProblems

  fun registerIgnoredProblem(problem: Problem,
                             ignoreDecisions: List<ProblemsFilter.Result.Ignore>) {
    if (problem !in ignoredProblems) {
      ignoredProblems.add(problem)
      ignoreDecisions.forEach { pluginVerificationReportage.logProblemIgnored(problem, it.reason) }
    }
  }
}
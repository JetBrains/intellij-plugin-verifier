package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem

/**
 * Holds the [CompatibilityProblem]s that have been ignored by [ProblemsFilter].
 */
class IgnoredProblemsHolder {

  val ignoredProblems = hashMapOf<CompatibilityProblem, List<ProblemsFilter.Result.Ignore>>()

  fun isIgnored(problem: CompatibilityProblem): Boolean =
      problem in ignoredProblems

  fun registerIgnoredProblem(
      problem: CompatibilityProblem,
      ignoreDecisions: List<ProblemsFilter.Result.Ignore>
  ) {
    if (problem !in ignoredProblems) {
      ignoredProblems[problem] = ignoreDecisions
    }
  }
}
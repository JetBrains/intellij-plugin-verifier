package com.jetbrains.pluginverifier.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Implementations of this interface can be used to exclude known and unrelated problems from verification results.
 */
interface ProblemsFilter {

  fun shouldReportProblem(problem: CompatibilityProblem, context: VerificationContext): Result

  sealed class Result {
    object Report : Result()

    data class Ignore(val reason: String) : Result()
  }
}
package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.filtering.ProblemsFilter.Result
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Implementations of this interface can be used
 * to exclude known and unrelated [problems] [CompatibilityProblem]
 * from the verifier [results] [Result].
 */
interface ProblemsFilter {

  fun shouldReportProblem(
      plugin: IdePlugin,
      ideVersion: IdeVersion,
      problem: CompatibilityProblem,
      verificationContext: VerificationContext
  ): Result

  sealed class Result {
    object Report : Result()

    data class Ignore(val reason: String) : Result()
  }
}
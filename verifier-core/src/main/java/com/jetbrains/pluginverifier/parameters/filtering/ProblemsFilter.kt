package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

interface ProblemsFilter {
  fun shouldReportProblem(plugin: IdePlugin, ideVersion: IdeVersion, problem: Problem, verificationContext: VerificationContext): Result

  sealed class Result {
    object Report : Result()

    data class Ignore(val reason: String) : Result()
  }
}
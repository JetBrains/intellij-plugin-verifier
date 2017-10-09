package com.jetbrains.pluginverifier.parameters.filtering

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.results.problems.Problem

interface ProblemsFilter {
  fun shouldReportProblem(plugin: IdePlugin, ideVersion: IdeVersion, problem: Problem): Result

  sealed class Result {
    object Report : Result()

    data class Ignore(val reason: String) : Result()
  }
}
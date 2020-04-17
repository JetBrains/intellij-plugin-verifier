/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

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
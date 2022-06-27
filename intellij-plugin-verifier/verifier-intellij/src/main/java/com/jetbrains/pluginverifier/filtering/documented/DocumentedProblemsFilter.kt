/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering.documented

import com.jetbrains.pluginverifier.filtering.ProblemsFilter
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * Implementation of the [ProblemsFilter] that drops
 * the problems documented on the
 * [Breaking API Changes page](https://plugins.jetbrains.com/docs/intellij/api-changes-list.html).
 */
class DocumentedProblemsFilter(private val documentedProblems: List<DocumentedProblem>) : ProblemsFilter {

  override fun shouldReportProblem(problem: CompatibilityProblem, context: VerificationContext): ProblemsFilter.Result {
    val documentedProblem = documentedProblems.find { it.isDocumenting(problem, context) }
    if (documentedProblem != null) {
      return ProblemsFilter.Result.Ignore("the problem is already documented in the API Breakages page (https://plugins.jetbrains.com/docs/intellij/api-changes-list.html)")
    }
    return ProblemsFilter.Result.Report
  }

}

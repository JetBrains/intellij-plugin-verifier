/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.filtering

import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.verifiers.PluginVerificationContext
import com.jetbrains.pluginverifier.verifiers.VerificationContext

/**
 * [ProblemsFilter] that keep only matched problems specified in [keepOnlyConditions]. All other problems will be ignored.
 */
class KeepOnlyProblemsFilter(val keepOnlyConditions: List<KeepOnlyCondition>) : ProblemsFilter {

    override fun shouldReportProblem(
        problem: CompatibilityProblem,
        context: VerificationContext
    ): ProblemsFilter.Result {
        if (context !is PluginVerificationContext) {
            return ProblemsFilter.Result.Report
        }

        val patterns = keepOnlyConditions.map { it.pattern }
        val doKeepProblem = patterns.any {
            problem.shortDescription.matches(it)
        }
        if (doKeepProblem.not()) {
            return ProblemsFilter.Result.Ignore("the problem is ignored as " +
                    "it's not matching any pattern for keeping problems : \"${patterns.joinToString()}\"")
        }
        return ProblemsFilter.Result.Report
    }

}
package com.jetbrains.pluginverifier.reporting.problems

import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.results.problems.Problem

/**
 * @author Sergey Patrikeev
 */
interface ProblemReporter : Reporter<Problem> {
  override fun report(t: Problem) = reportProblem(t)

  fun reportProblem(problem: Problem)
}
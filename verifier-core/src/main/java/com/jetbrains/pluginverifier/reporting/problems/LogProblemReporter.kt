package com.jetbrains.pluginverifier.reporting.problems

import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.results.problems.Problem
import org.slf4j.Logger

class LogProblemReporter(logger: Logger) : LogReporter<Problem>(logger), ProblemReporter {
  override fun reportProblem(problem: Problem) {
    super<LogReporter>.report(problem)
  }

  override fun report(t: Problem) = reportProblem(t)
}
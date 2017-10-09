package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.pluginverifier.reporting.common.LogReporter
import org.slf4j.Logger

class LogIgnoredProblemReporter(logger: Logger) : LogReporter<String>(logger), IgnoredProblemReporter {
  override fun reportIgnoredProblem(ignoredDescription: String) {
    super<LogReporter>.report(ignoredDescription)
  }

  override fun report(t: String) = reportIgnoredProblem(t)
}
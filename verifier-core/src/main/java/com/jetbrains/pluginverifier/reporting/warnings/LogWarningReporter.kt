package com.jetbrains.pluginverifier.reporting.warnings

import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.results.warnings.Warning
import org.slf4j.Logger

class LogWarningReporter(logger: Logger) : WarningReporter, LogReporter<Warning>(logger) {
  override fun reportWarning(warning: Warning) {
    super<LogReporter>.report(warning)
  }

  override fun report(t: Warning) = reportWarning(t)
}
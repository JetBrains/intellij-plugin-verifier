package com.jetbrains.pluginverifier.reporting.verdict

import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.results.Verdict
import org.slf4j.Logger

class LogVerdictReporter(logger: Logger) : VerdictReporter, LogReporter<Verdict>(logger) {
  override fun reportVerdict(verdict: Verdict) {
    super<LogReporter>.report(verdict)
  }

  override fun report(t: Verdict) = reportVerdict(t)
}
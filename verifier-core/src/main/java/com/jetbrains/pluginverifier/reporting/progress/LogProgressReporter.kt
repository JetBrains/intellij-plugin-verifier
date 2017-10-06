package com.jetbrains.pluginverifier.reporting.progress

import com.jetbrains.pluginverifier.reporting.common.LogReporter
import org.slf4j.Logger

class LogProgressReporter(logger: Logger,
                          private val logMessageProvider: (Double) -> String,
                          private val step: Double) : ProgressReporter, LogReporter<Double>(logger) {
  private var lastCompleted: Double = 0.0

  override fun reportProgress(completed: Double) {
    require(completed in 0.0..1.0)
    if ((completed == 1.0 && lastCompleted != 1.0) || completed - lastCompleted >= step) {
      lastCompleted = completed
      super.reportLine(logMessageProvider(completed))
    }
  }

  override fun report(t: Double) = reportProgress(t)

  override fun close() {
    if (lastCompleted != 1.0) {
      report(1.0)
    }
  }
}
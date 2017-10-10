package com.jetbrains.pluginverifier.reporting.progress

import com.jetbrains.pluginverifier.reporting.common.LogReporter
import org.slf4j.Logger

class LogSteppedProgressReporter(logger: Logger,
                                 private val logMessageProvider: (Double) -> String,
                                 private val step: Double) : LogReporter<Double>(logger) {

  private var lastCompleted: Double = 0.0

  override fun report(t: Double) {
    require(t in 0.0..1.0)
    if ((t == 1.0 && lastCompleted != 1.0) || t - lastCompleted >= step) {
      lastCompleted = t
      super.reportLine(logMessageProvider(t))
    }
  }

  override fun close() {
    if (lastCompleted != 1.0) {
      report(1.0)
    }
  }
}
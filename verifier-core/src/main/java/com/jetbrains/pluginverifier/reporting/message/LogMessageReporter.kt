package com.jetbrains.pluginverifier.reporting.message

import com.jetbrains.pluginverifier.reporting.common.LogReporter
import org.slf4j.Logger

class LogMessageReporter(logger: Logger) : LogReporter<String>(logger), MessageReporter {
  override fun reportMessage(message: String) {
    super<LogReporter>.report(message)
  }

  override fun report(t: String) {
    reportMessage(t)
  }
}
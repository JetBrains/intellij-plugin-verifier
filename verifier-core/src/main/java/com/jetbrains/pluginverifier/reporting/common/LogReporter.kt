package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter
import org.slf4j.Logger

open class LogReporter<in T>(protected val logger: Logger) : Reporter<T> {
  override fun close() = Unit

  override fun report(t: T) {
    logger.info(t.toString())
  }

  fun reportLine(line: String) {
    logger.info(line)
  }
}
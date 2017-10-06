package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter
import org.slf4j.Logger

/**
 * @author Sergey Patrikeev
 */
open class LogReporter<T>(private val logger: Logger,
                          private val lineProvider: (T) -> String = { it.toString() }) : Reporter<T> {
  override fun close() = Unit

  override fun report(t: T) {
    val line = lineProvider(t)
    reportLine(line)
  }

  fun reportLine(line: String) {
    logger.info(line)
  }
}
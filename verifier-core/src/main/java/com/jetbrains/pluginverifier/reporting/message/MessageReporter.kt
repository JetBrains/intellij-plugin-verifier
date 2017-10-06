package com.jetbrains.pluginverifier.reporting.message

import com.jetbrains.pluginverifier.reporting.Reporter

/**
 * @author Sergey Patrikeev
 */
interface MessageReporter : Reporter<String> {
  override fun report(t: String) = reportMessage(t)

  fun reportMessage(message: String)
}
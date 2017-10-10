package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter

/**
 * @author Sergey Patrikeev
 */
class CloseIgnoringReporter<in T>(private val delegateTo: Reporter<T>) : Reporter<T> {
  override fun report(t: T) = delegateTo.report(t)

  override fun close() = Unit
}
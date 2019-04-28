package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter

/**
 * Reporter that ignores [close] called on [delegate].
 */
class CloseIgnoringDelegateReporter<T>(private val delegate: Reporter<T>) : Reporter<T> {
  override fun report(t: T) {
    delegate.report(t)
  }

  override fun close() = Unit
}
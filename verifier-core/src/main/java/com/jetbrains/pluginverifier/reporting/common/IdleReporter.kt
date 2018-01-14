package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.pluginverifier.reporting.Reporter

class IdleReporter<T> : Reporter<T> {
  override fun report(t: T) = Unit

  override fun close() = Unit
}
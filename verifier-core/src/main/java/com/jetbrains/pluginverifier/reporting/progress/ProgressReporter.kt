package com.jetbrains.pluginverifier.reporting.progress

import com.jetbrains.pluginverifier.reporting.Reporter

/**
 * @author Sergey Patrikeev
 */
interface ProgressReporter : Reporter<Double> {
  fun reportProgress(completed: Double)

  override fun report(t: Double) = reportProgress(t)
}
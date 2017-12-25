package org.jetbrains.plugins.verifier.service.service.verifier

import com.jetbrains.pluginverifier.reporting.Reporter
import org.jetbrains.plugins.verifier.service.tasks.ProgressIndicator

class DelegateProgressReporter(private val progressIndicator: ProgressIndicator) : Reporter<Double> {
  override fun report(t: Double) {
    progressIndicator.fraction = t
  }

  override fun close() = Unit
}
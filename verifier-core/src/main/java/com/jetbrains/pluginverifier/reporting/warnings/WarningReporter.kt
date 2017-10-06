package com.jetbrains.pluginverifier.reporting.warnings

import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.results.warnings.Warning

/**
 * @author Sergey Patrikeev
 */
interface WarningReporter : Reporter<Warning> {

  fun reportWarning(warning: Warning)

  override fun report(t: Warning) = reportWarning(t)
}
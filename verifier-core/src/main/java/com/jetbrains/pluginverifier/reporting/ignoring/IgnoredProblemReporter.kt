package com.jetbrains.pluginverifier.reporting.ignoring

import com.jetbrains.pluginverifier.reporting.Reporter

/**
 * @author Sergey Patrikeev
 */
interface IgnoredProblemReporter : Reporter<String> {
  override fun report(t: String) = reportIgnoredProblem(t)

  fun reportIgnoredProblem(ignoredDescription: String)
}
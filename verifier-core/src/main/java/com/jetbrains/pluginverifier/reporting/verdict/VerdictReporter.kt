package com.jetbrains.pluginverifier.reporting.verdict

import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.results.Verdict

/**
 * @author Sergey Patrikeev
 */
interface VerdictReporter : Reporter<Verdict> {
  override fun report(t: Verdict) = reportVerdict(t)

  fun reportVerdict(verdict: Verdict)
}
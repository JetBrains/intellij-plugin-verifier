package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.reporting.dependencies.DependencyGraphReporter
import com.jetbrains.pluginverifier.reporting.ignoring.IgnoredProblemReporter
import com.jetbrains.pluginverifier.reporting.message.MessageReporter
import com.jetbrains.pluginverifier.reporting.problems.ProblemReporter
import com.jetbrains.pluginverifier.reporting.progress.ProgressReporter
import com.jetbrains.pluginverifier.reporting.verdict.VerdictReporter
import com.jetbrains.pluginverifier.reporting.warnings.WarningReporter
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
data class ReporterSet(
    val verdictReporters: List<VerdictReporter>,
    val messageReporters: List<MessageReporter>,
    val progressReporters: List<ProgressReporter>,
    val warningReporters: List<WarningReporter>,
    val problemsReporters: List<ProblemReporter>,
    val dependenciesGraphReporters: List<DependencyGraphReporter>,
    val ignoredProblemReporters: List<IgnoredProblemReporter>
) : Closeable {
  override fun close() {
    verdictReporters.forEach { it.closeLogged() }
    messageReporters.forEach { it.closeLogged() }
    progressReporters.forEach { it.closeLogged() }
    problemsReporters.forEach { it.closeLogged() }
    warningReporters.forEach { it.closeLogged() }
    dependenciesGraphReporters.forEach { it.closeLogged() }
    ignoredProblemReporters.forEach { it.closeLogged() }
  }
}
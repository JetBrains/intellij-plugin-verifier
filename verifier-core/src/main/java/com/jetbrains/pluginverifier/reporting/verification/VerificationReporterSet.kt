package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.MessageAndException
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import java.io.Closeable

/**
 * Set of configured [reporters] [Reporter] used
 * by the [PluginVerificationReportage] to report and save the
 * verification stages and results.
 */
data class VerificationReporterSet(
    val verificationResultReporters: List<Reporter<VerificationResult>>,
    val messageReporters: List<Reporter<String>>,
    val progressReporters: List<Reporter<Double>>,
    val warningReporters: List<Reporter<PluginProblem>>,
    val problemsReporters: List<Reporter<Problem>>,
    val dependenciesGraphReporters: List<Reporter<DependenciesGraph>>,
    val ignoredProblemReporters: List<Reporter<ProblemIgnoredEvent>>,
    val deprecatedReporters: List<Reporter<DeprecatedApiUsage>>,
    val exceptionReporters: List<Reporter<MessageAndException>>
) : Closeable {
  override fun close() {
    verificationResultReporters.forEach { it.closeLogged() }
    messageReporters.forEach { it.closeLogged() }
    progressReporters.forEach { it.closeLogged() }
    problemsReporters.forEach { it.closeLogged() }
    warningReporters.forEach { it.closeLogged() }
    dependenciesGraphReporters.forEach { it.closeLogged() }
    ignoredProblemReporters.forEach { it.closeLogged() }
    deprecatedReporters.forEach { it.closeLogged() }
    exceptionReporters.forEach { it.closeLogged() }
  }
}
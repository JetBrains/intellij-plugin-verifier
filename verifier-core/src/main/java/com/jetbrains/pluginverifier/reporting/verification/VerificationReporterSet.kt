package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.MessageAndException
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
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
    val pluginStructureWarningsReporters: List<Reporter<PluginStructureWarning>>,
    val pluginStructureErrorsReporters: List<Reporter<PluginStructureError>>,
    val problemsReporters: List<Reporter<CompatibilityProblem>>,
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
    pluginStructureWarningsReporters.forEach { it.closeLogged() }
    pluginStructureErrorsReporters.forEach { it.closeLogged() }
    dependenciesGraphReporters.forEach { it.closeLogged() }
    ignoredProblemReporters.forEach { it.closeLogged() }
    deprecatedReporters.forEach { it.closeLogged() }
    exceptionReporters.forEach { it.closeLogged() }
  }
}
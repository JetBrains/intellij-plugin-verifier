package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.reporting.common.MessageAndException
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import java.io.Closeable

/**
 * Set of configured [reporters] [Reporter] used
 * by the [Reporters] to report and save the
 * verification stages and results.
 */
data class Reporters(
    /**
     * Reporters of [VerificationResult]s.
     */
    val verificationResultReporters: List<Reporter<VerificationResult>> = emptyList(),
    /**
     * Reporters of execution messages.
     */
    val messageReporters: List<Reporter<String>> = emptyList(),
    /**
     * Reporters of progress
     */
    val progressReporters: List<Reporter<Double>> = emptyList(),
    /**
     * Reporters of plugins' warnings [PluginStructureWarning]
     */
    val pluginStructureWarningsReporters: List<Reporter<PluginStructureWarning>> = emptyList(),
    /**
     * Reporters of plugins' errors [PluginStructureError]
     */
    val pluginStructureErrorsReporters: List<Reporter<PluginStructureError>> = emptyList(),
    /**
     * Reporters of compatibility problems [CompatibilityProblem]
     */
    val problemsReporters: List<Reporter<CompatibilityProblem>> = emptyList(),
    /**
     * Reporters of dependencies graphs [DependenciesGraph]
     */
    val dependenciesGraphReporters: List<Reporter<DependenciesGraph>> = emptyList(),
    /**
     * Reporters of ignored problems [ProblemIgnoredEvent]
     */
    val ignoredProblemReporters: List<Reporter<ProblemIgnoredEvent>> = emptyList(),
    /**
     * Reporters of deprecated API usages
     */
    val deprecatedReporters: List<Reporter<DeprecatedApiUsage>> = emptyList(),
    /**
     * Reporters of exceptions occurred.
     */
    val exceptionReporters: List<Reporter<MessageAndException>> = emptyList(),
    /**
     * Reporters of experimental API usages..
     */
    val experimentalApiUsageReporters: List<Reporter<ExperimentalApiUsage>> = emptyList()
) : Closeable {

  fun reportException(message: String, exception: Throwable) {
    exceptionReporters.forEach { it.report(MessageAndException(message, exception)) }
  }

  fun reportProgress(completed: Double) {
    progressReporters.forEach { it.report(completed) }
  }

  fun reportNewProblemDetected(problem: CompatibilityProblem) {
    problemsReporters.forEach { it.report(problem) }
  }

  fun reportNewPluginStructureWarning(pluginStructureWarning: PluginStructureWarning) {
    pluginStructureWarningsReporters.forEach { it.report(pluginStructureWarning) }
  }

  fun reportNewPluginStructureError(pluginStructureError: PluginStructureError) {
    pluginStructureErrorsReporters.forEach { it.report(pluginStructureError) }
  }

  fun reportDependencyGraph(dependenciesGraph: DependenciesGraph) {
    dependenciesGraphReporters.forEach { it.report(dependenciesGraph) }
  }

  fun reportVerificationResult(verificationResult: VerificationResult) {
    verificationResultReporters.forEach { it.report(verificationResult) }
  }

  fun reportMessage(message: String) {
    messageReporters.forEach { it.report(message) }
  }

  fun reportProblemIgnored(problemIgnoredEvent: ProblemIgnoredEvent) {
    ignoredProblemReporters.forEach { it.report(problemIgnoredEvent) }
  }

  fun reportDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    deprecatedReporters.forEach { it.report(deprecatedApiUsage) }
  }

  fun reportExperimentalApi(experimentalApiUsage: ExperimentalApiUsage) {
    experimentalApiUsageReporters.forEach { it.report(experimentalApiUsage) }
  }

  /**
   * Closes all the reporters and releases the allocated resources.
   */
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
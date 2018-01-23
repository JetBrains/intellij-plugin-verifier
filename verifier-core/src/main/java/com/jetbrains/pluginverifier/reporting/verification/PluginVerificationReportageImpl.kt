package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.reporting.common.MessageAndException
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.results.structure.PluginStructureError
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning

class PluginVerificationReportageImpl(private val verificationReportage: VerificationReportage,
                                      override val plugin: PluginInfo,
                                      override val ideVersion: IdeVersion,
                                      private val reporterSet: VerificationReporterSet) : PluginVerificationReportage {
  @Volatile
  private var startTime: Long = 0

  override fun logVerificationStarted() {
    reportMessage("Start verification of $plugin against $ideVersion")
    startTime = System.currentTimeMillis()
  }

  override fun logVerificationFinished(message: String) {
    val elapsedTime = System.currentTimeMillis() - startTime
    reportMessage("Finished verification of $plugin against $ideVersion in ${"%.2f".format(elapsedTime / 1000.0)} seconds: $message")
    verificationReportage.logPluginVerificationFinished(this)
  }

  override fun logException(message: String, exception: Throwable) {
    reporterSet.exceptionReporters.forEach { it.report(MessageAndException(message, exception)) }
  }

  override fun logProgress(completed: Double) {
    reporterSet.progressReporters.forEach { it.report(completed) }
  }

  override fun logNewProblemDetected(problem: CompatibilityProblem) {
    reporterSet.problemsReporters.forEach { it.report(problem) }
  }

  override fun logNewPluginStructureWarning(pluginStructureWarning: PluginStructureWarning) {
    reporterSet.pluginStructureWarningsReporters.forEach { it.report(pluginStructureWarning) }
  }

  override fun logNewPluginStructureError(pluginStructureError: PluginStructureError) {
    reporterSet.pluginStructureErrorsReporters.forEach { it.report(pluginStructureError) }
  }

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) {
    reporterSet.dependenciesGraphReporters.forEach { it.report(dependenciesGraph) }
  }

  override fun logVerificationResult(verificationResult: VerificationResult) {
    reporterSet.verificationResultReporters.forEach { it.report(verificationResult) }
  }

  private fun reportMessage(message: String) {
    reporterSet.messageReporters.forEach { it.report(message) }
  }

  override fun logProblemIgnored(problem: CompatibilityProblem, reason: String) {
    reporterSet.ignoredProblemReporters.forEach { it.report(ProblemIgnoredEvent(plugin, ideVersion, problem, reason)) }
  }

  override fun logDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    reporterSet.deprecatedReporters.forEach { it.report(deprecatedApiUsage) }
  }

  override fun close() {
    reporterSet.close()
  }

}
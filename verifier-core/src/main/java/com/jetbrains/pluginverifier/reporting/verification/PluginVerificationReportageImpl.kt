package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.reporting.ignoring.ProblemIgnoredEvent
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedApiUsage
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

class PluginVerificationReportageImpl(private val verificationReportage: VerificationReportage,
                                      override val plugin: PluginInfo,
                                      override val ideVersion: IdeVersion,
                                      private val reporterSet: VerificationReporterSet) : PluginVerificationReportage {
  @Volatile
  private var startTime: Long = 0

  override fun logVerificationStarted() {
    reportMessage("Start verification of $plugin with $ideVersion")
    startTime = System.currentTimeMillis()
  }

  override fun logVerificationFinished(result: Result) {
    val elapsedTime = System.currentTimeMillis() - startTime
    reportMessage("Finished in ${"%.2f".format(elapsedTime / 1000.0)} seconds: ${result.verdict}")
    verificationReportage.logPluginVerificationFinished(this)
  }

  override fun logProgress(completed: Double) {
    reporterSet.progressReporters.forEach { it.report(completed) }
  }

  override fun logNewProblemDetected(problem: Problem) {
    reporterSet.problemsReporters.forEach { it.report(problem) }
  }

  override fun logNewWarningDetected(warning: Warning) {
    reporterSet.warningReporters.forEach { it.report(warning) }
  }

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) {
    reporterSet.dependenciesGraphReporters.forEach { it.report(dependenciesGraph) }
  }

  override fun logVerdict(verdict: Verdict) {
    reporterSet.verdictReporters.forEach { it.report(verdict) }
  }

  private fun reportMessage(message: String) {
    reporterSet.messageReporters.forEach { it.report(message) }
  }

  override fun logProblemIgnored(problem: Problem, reason: String) {
    reporterSet.ignoredProblemReporters.forEach { it.report(ProblemIgnoredEvent(plugin, ideVersion, problem, reason)) }
  }

  override fun logDeprecatedUsage(deprecatedApiUsage: DeprecatedApiUsage) {
    reporterSet.deprecatedReporters.forEach { it.report(deprecatedApiUsage) }
  }

  override fun close() {
    reporterSet.close()
  }

}
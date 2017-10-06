package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

class PluginVerificationReportageImpl(private val verificationReportage: VerificationReportage,
                                      override val plugin: PluginCoordinate,
                                      override val ideVersion: IdeVersion,
                                      private val reporterSet: ReporterSet) : PluginVerificationReportage {

  private var startTime: Long = 0

  override fun logVerificationStarted() {
    reportMessage("Verification of $plugin with $ideVersion is starting")
    startTime = System.currentTimeMillis()
  }

  override fun logVerificationFinished() {
    val elapsedTime = System.currentTimeMillis() - startTime
    reportMessage("Verification of $plugin with $ideVersion is finished in ${"%.2f".format((elapsedTime / 1000).toDouble())} seconds")
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

  override fun close() {
    reporterSet.close()
  }

}
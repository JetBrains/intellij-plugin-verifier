package com.jetbrains.pluginverifier.logging

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.results.problems.Problem
import com.jetbrains.pluginverifier.results.warnings.Warning

class PluginLoggerImpl(private val verificationLogger: VerificationLogger,
                       override val plugin: PluginCoordinate,
                       override val ideVersion: IdeVersion) : PluginLogger {
  override fun logCompletedClasses(fraction: Double) {
    info("Verification of '$plugin' and #$ideVersion finished " + "%.2f".format(fraction) + "% of classes")
  }

  override fun logNewProblemDetected(problem: Problem) {

  }

  override fun logNewWarningDetected(warning: Warning) {

  }

  override fun info(message: String) {

  }

  override fun info(message: String, e: Throwable?) {

  }

  override fun error(message: String, e: Throwable?) {

  }

  private var startTime: Long = 0

  override fun logVerificationStarted() {
    info("Verification of $plugin with $ideVersion is starting")
    startTime = System.currentTimeMillis()
  }

  override fun logVerificationFinished() {
    val elapsedTime = System.currentTimeMillis() - startTime
    info("Verification of $plugin with $ideVersion is finished in ${elapsedTime / 1000} seconds")
  }

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) {
    info("Dependencies graph for $plugin: $dependenciesGraph")
  }

  override fun logVerdict(verdict: Verdict) {
    verificationLogger.logPluginVerificationFinished(this)
  }

}
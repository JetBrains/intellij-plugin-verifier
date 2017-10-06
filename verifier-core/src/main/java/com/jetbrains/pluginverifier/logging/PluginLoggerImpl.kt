package com.jetbrains.pluginverifier.logging

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.logging.loggers.Logger
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.results.Verdict

class PluginLoggerImpl(val verificationLogger: VerificationLoggerImpl,
                       override val plugin: PluginCoordinate,
                       override val ideVersion: IdeVersion,
                       private val logger: Logger) : PluginLogger {
  override fun info(message: String) {

  }

  override fun info(message: String, e: Throwable?) {

  }

  override fun error(message: String, e: Throwable?) {

  }

  private var startTime: Long = 0

  private var verdict: Verdict? = null

  override fun started() {
    logger.info("Verification of $plugin with $ideVersion is starting")
    startTime = System.currentTimeMillis()
  }

  override fun finished() {
    val elapsedTime = System.currentTimeMillis() - startTime
    logger.info("Verification of $plugin with $ideVersion is finished in ${elapsedTime / 1000} seconds")

    if (verdict != null) {
      verificationLogger.pluginFinished(plugin, ideVersion, verdict!!)
    }
  }

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) {
    logger.info("Dependencies graph for $plugin: $dependenciesGraph")
  }

  override fun logVerdict(verdict: Verdict) {
    this.verdict = verdict
  }

}
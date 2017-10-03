package com.jetbrains.pluginverifier.logging

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.logging.loggers.Logger
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

class PluginLoggerImpl(val logger: VerificationLoggerImpl,
                       override val plugin: PluginCoordinate,
                       override val ideVersion: IdeVersion,
                       private val pluginLogger: Logger) : PluginLogger {

  private var startTime: Long = 0

  private var verdict: Verdict? = null

  override fun started() {
    pluginLogger.info("Verification of $plugin with $ideVersion is starting")
    startTime = System.currentTimeMillis()
  }

  override fun finished() {
    val elapsedTime = System.currentTimeMillis() - startTime
    pluginLogger.info("Verification of $plugin with $ideVersion is finished in ${elapsedTime / 1000} seconds")

    if (verdict != null) {
      logger.pluginFinished(plugin, ideVersion, verdict!!)
    }
  }

  override fun logDependencyGraph(dependenciesGraph: DependenciesGraph) {
    pluginLogger.info("Dependencies graph for $plugin: $dependenciesGraph")
  }

  override fun logVerdict(verdict: Verdict) {
    this.verdict = verdict
  }

}
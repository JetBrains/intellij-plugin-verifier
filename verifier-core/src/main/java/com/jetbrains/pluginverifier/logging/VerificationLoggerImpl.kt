package com.jetbrains.pluginverifier.logging

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.Verdict
import com.jetbrains.pluginverifier.logging.loggers.Logger
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

class VerificationLoggerImpl(private val logger: Logger) : VerificationLogger {

  @Volatile
  private var verified: Int = 0

  override var tasksNumber: Int = 0

  internal fun pluginFinished(pluginInfo: PluginCoordinate, ideVersion: IdeVersion, verdict: Verdict) {
    ++verified
    logEvent("$verified of $tasksNumber finished. Plugin $pluginInfo and $ideVersion: $verdict")
  }

  override fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginLogger {
    tasksNumber++
    val subLogger = logger.createSubLogger(pluginCoordinate.uniqueId)
    return PluginLoggerImpl(this, pluginCoordinate, ideDescriptor.ideVersion, subLogger)
  }

  override fun logEvent(message: String) {
    logger.info(message)
  }
}
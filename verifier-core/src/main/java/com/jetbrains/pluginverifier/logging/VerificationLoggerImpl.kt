package com.jetbrains.pluginverifier.logging

import com.jetbrains.pluginverifier.logging.loggers.Logger
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

class VerificationLoggerImpl(private val logger: Logger) : VerificationLogger {
  @Volatile
  private var verified: Int = 0

  private var tasksNumber: Int = 0

  @Synchronized
  override fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginLogger {
    tasksNumber++
    val subLogger = logger.createSubLogger(pluginCoordinate.uniqueId)
    return PluginLoggerImpl(this, pluginCoordinate, ideDescriptor.ideVersion)
  }

  override fun logEvent(message: String) {
    logger.info(message)
  }

  override fun logPluginVerificationFinished(pluginLogger: PluginLogger) {
    ++verified
    logEvent("$verified of $tasksNumber finished. Plugin ${pluginLogger.plugin} and ${pluginLogger.ideVersion}")
  }
}
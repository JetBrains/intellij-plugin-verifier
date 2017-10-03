package com.jetbrains.pluginverifier.logging

import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

/**
 * @author Sergey Patrikeev
 */
interface VerificationLogger {

  var tasksNumber: Int

  fun logEvent(message: String)

  fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginLogger

}
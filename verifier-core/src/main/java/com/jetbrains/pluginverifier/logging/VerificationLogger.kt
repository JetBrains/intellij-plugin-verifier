package com.jetbrains.pluginverifier.logging

import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

/**
 * @author Sergey Patrikeev
 */
interface VerificationLogger {

  fun logEvent(message: String)

  fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginLogger

}
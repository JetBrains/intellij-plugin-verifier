package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate

/**
 * @author Sergey Patrikeev
 */
interface VerificationReportage {

  fun logVerificationExecutorCreated(availableMemory: Long, availableCpu: Long, concurrencyLevel: Int)

  fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginVerificationReportage

  fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage)

}
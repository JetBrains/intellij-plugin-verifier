package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
interface VerificationReportage : Closeable {

  fun logVerificationExecutorCreated(availableMemory: Long, availableCpu: Long, concurrencyLevel: Int)

  fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginVerificationReportage

  fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage)

}
package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
interface VerificationReportage : Closeable {

  fun logVerificationExecutorCreated(availableMemory: SpaceAmount, availableCpu: Long, concurrencyLevel: Int)

  fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginVerificationReportage

  fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage)

  fun logVerificationStage(stageMessage: String)

}
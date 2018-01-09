package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.Closeable

/**
 * Allows to report, log and save the verification stages and results in a configurable way.
 */
interface VerificationReportage : Closeable {

  fun createPluginReportage(pluginInfo: PluginInfo, ideDescriptor: IdeDescriptor): PluginVerificationReportage

  fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage)

  fun logVerificationStage(stageMessage: String)

}
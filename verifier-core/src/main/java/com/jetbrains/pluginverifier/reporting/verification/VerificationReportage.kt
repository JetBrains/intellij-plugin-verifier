package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.Closeable

/**
 * Allows to report, log and save the verification stages and results in a configurable way.
 */
interface VerificationReportage : Closeable {

  fun createPluginReportage(pluginInfo: PluginInfo, ideVersion: IdeVersion): PluginVerificationReportage

  fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage)

  fun logVerificationStage(stageMessage: String)

}
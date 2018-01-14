package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.Closeable

/**
 * Allows to report, log and save the verification stages and results in a configurable way.
 */
interface VerificationReportage : Closeable {

  /**
   * Creates a [PluginVerificationReportage] for saving the reports
   * of the verification of the [pluginInfo] against [ideVersion].
   */
  fun createPluginReportage(pluginInfo: PluginInfo, ideVersion: IdeVersion): PluginVerificationReportage

  /**
   * Invoked when the verification finishes.
   */
  fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage)

  /**
   * Logs the verification stage.
   */
  fun logVerificationStage(stageMessage: String)

}
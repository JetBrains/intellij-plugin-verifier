package com.jetbrains.pluginverifier.reporting

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.Closeable

/**
 * Allows to report, log and save the verification stages and results in a configurable way.
 */
interface PluginVerificationReportage : Closeable {

  /**
   * Logs the verification stage.
   */
  fun logVerificationStage(stageMessage: String)

  /**
   * Logs that the verification of [pluginInfo] against [verificationTarget] is ignored due to some [reason].
   */
  fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: PluginVerificationTarget, reason: String)

  /**
   * Report verification result.
   */
  fun reportVerificationResult(pluginVerificationResult: PluginVerificationResult)

}
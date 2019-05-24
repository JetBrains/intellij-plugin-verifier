package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Provides [Reporters]s for individual plugins' verifications.
 */
interface ReportersProvider {

  /**
   * Provides [Reporters] used in the [Reporters]
   * for reporting the verification stages, progress and results
   * of verifying the [pluginInfo] against [verificationTarget].
   */
  fun getPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget): Reporters
}
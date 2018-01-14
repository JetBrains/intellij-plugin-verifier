package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.reporting.Reporter
import com.jetbrains.pluginverifier.repository.PluginInfo
import java.io.Closeable

/**
 * Configures the [Reporter]s used to report
 * the global verification messages
 * and to configure the [PluginVerificationReportage]s
 * of the plugins' verifications.
 */
interface VerificationReportersProvider : Closeable {

  /**
   * [Reporter]s for reporting the global verification
   * stages and messages.
   */
  val globalMessageReporters: List<Reporter<String>>

  /**
   * [Reporter]s for reporting the glibal verification progress.
   */
  val globalProgressReporters: List<Reporter<Double>>

  /**
   * Provides a [VerificationReporterSet] used in the [PluginVerificationReportage]
   * for reporting the verification stages, progress and results
   * of verifying the [pluginInfo] against [ideVersion].
   */
  fun getReporterSetForPluginVerification(pluginInfo: PluginInfo, ideVersion: IdeVersion): VerificationReporterSet
}
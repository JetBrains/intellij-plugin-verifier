package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.reporting.common.LogReporter
import com.jetbrains.pluginverifier.reporting.ignoring.IgnoredPluginsReporter
import com.jetbrains.pluginverifier.reporting.ignoring.PluginIgnoredEvent
import com.jetbrains.pluginverifier.repository.PluginInfo

/**
 * Main implementation of [VerificationReportage].
 */
class ReportageImpl(
    val reportersProvider: ReportersProvider,
    val messageReporters: List<LogReporter<String>>,
    val ignoredPluginsReporter: IgnoredPluginsReporter
) : VerificationReportage {

  override fun logVerificationStage(stageMessage: String) {
    messageReporters.forEach { it.report(stageMessage) }
  }

  override fun logPluginVerificationIgnored(
      pluginInfo: PluginInfo,
      verificationTarget: VerificationTarget,
      reason: String
  ) {
    ignoredPluginsReporter.report(PluginIgnoredEvent(pluginInfo, verificationTarget, reason))
  }

  @Synchronized
  override fun createPluginReporters(pluginInfo: PluginInfo, verificationTarget: VerificationTarget): Reporters {
    return reportersProvider.getPluginReporters(pluginInfo, verificationTarget)
  }

  override fun close() {
    messageReporters.forEach { it.closeLogged() }
    ignoredPluginsReporter.closeLogged()
  }

}
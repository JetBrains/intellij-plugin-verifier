package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.reporting.ignoring.PluginIgnoredEvent
import com.jetbrains.pluginverifier.repository.PluginInfo

class VerificationReportageImpl(private val reporterSetProvider: VerificationReportersProvider) : VerificationReportage {
  private var verified = 0

  private var totalTasks = 0

  private fun reportMessage(message: String) {
    reporterSetProvider.globalMessageReporters.forEach { it.report(message) }
  }

  override fun logVerificationStage(stageMessage: String) {
    reportMessage(stageMessage)
  }

  override fun logPluginVerificationIgnored(pluginInfo: PluginInfo, verificationTarget: VerificationTarget, reason: String) {
    val pluginIgnoredEvent = PluginIgnoredEvent(pluginInfo, verificationTarget, reason)
    reporterSetProvider.ignoredPluginsReporters.forEach { it.report(pluginIgnoredEvent) }
  }

  @Synchronized
  override fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage) {
    ++verified
    reportMessage("$verified of $totalTasks tasks finished: ${pluginVerificationReportage.plugin} against ${pluginVerificationReportage.verificationTarget}")
    pluginVerificationReportage.closeLogged()
    reporterSetProvider.globalProgressReporters.forEach { it.report(verified.toDouble() / totalTasks) }
  }

  @Synchronized
  override fun createPluginReportage(pluginInfo: PluginInfo, verificationTarget: VerificationTarget): PluginVerificationReportage {
    totalTasks++
    val reporterSet = reporterSetProvider.getReporterSetForPluginVerification(pluginInfo, verificationTarget)
    return PluginVerificationReportageImpl(this, pluginInfo, verificationTarget, reporterSet)
  }

  override fun close() {
    reporterSetProvider.close()
  }

}
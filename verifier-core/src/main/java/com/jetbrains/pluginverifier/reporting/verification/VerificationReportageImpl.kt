package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
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

  override fun logPluginVerificationIgnored(pluginInfo: PluginInfo, ideVersion: IdeVersion, reason: String) {
    val pluginIgnoredEvent = PluginIgnoredEvent(pluginInfo, ideVersion, reason)
    reporterSetProvider.ignoredPluginsReporters.forEach { it.report(pluginIgnoredEvent) }
  }

  @Synchronized
  override fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage) {
    ++verified
    reportMessage("$verified of $totalTasks tasks finished: ${pluginVerificationReportage.plugin} and #${pluginVerificationReportage.ideVersion}")
    pluginVerificationReportage.closeLogged()
    reporterSetProvider.globalProgressReporters.forEach { it.report(verified.toDouble() / totalTasks) }
  }

  @Synchronized
  override fun createPluginReportage(pluginInfo: PluginInfo, ideVersion: IdeVersion): PluginVerificationReportage {
    totalTasks++
    val reporterSet = reporterSetProvider.getReporterSetForPluginVerification(pluginInfo, ideVersion)
    return PluginVerificationReportageImpl(this, pluginInfo, ideVersion, reporterSet)
  }

  override fun close() {
    reporterSetProvider.close()
  }

}
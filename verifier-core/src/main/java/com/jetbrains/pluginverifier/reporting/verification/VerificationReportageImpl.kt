package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.PluginInfo

class VerificationReportageImpl(private val reporterSetProvider: VerificationReportersProvider) : VerificationReportage {
  private var verifiedPlugins: Int = 0

  private var totalPlugins: Int = 0

  private fun reportMessage(message: String) {
    reporterSetProvider.globalMessageReporters.forEach { it.report(message) }
  }

  override fun logVerificationStage(stageMessage: String) {
    reportMessage(stageMessage)
  }

  @Synchronized
  override fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage) {
    ++verifiedPlugins
    reportMessage("$verifiedPlugins of $totalPlugins plugins finished: ${pluginVerificationReportage.plugin} and #${pluginVerificationReportage.ideVersion}")
    pluginVerificationReportage.closeLogged()
    reporterSetProvider.globalProgressReporters.forEach { it.report(verifiedPlugins.toDouble() / totalPlugins) }
  }

  @Synchronized
  override fun createPluginReportage(pluginInfo: PluginInfo, ideVersion: IdeVersion): PluginVerificationReportage {
    totalPlugins++
    val reporterSet = reporterSetProvider.getReporterSetForPluginVerification(pluginInfo, ideVersion)
    return PluginVerificationReportageImpl(this, pluginInfo, ideVersion, reporterSet)
  }

  override fun close() {
    reporterSetProvider.close()
  }

}
package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.ide.IdeDescriptor
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount

class VerificationReportageImpl(private val reporterSetProvider: VerificationReportersProvider) : VerificationReportage {
  private var verifiedPlugins: Int = 0

  private var totalPlugins: Int = 0

  override fun logVerificationExecutorCreated(availableMemory: SpaceAmount, availableCpu: Long, concurrencyLevel: Int) {
    reportMessage("Available memory: $availableMemory; Available CPU = $availableCpu; Concurrency level = $concurrencyLevel")
  }

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
  override fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginVerificationReportage {
    totalPlugins++
    val reporterSet = reporterSetProvider.getReporterSetForPluginVerification(pluginCoordinate, ideDescriptor.ideVersion)
    return PluginVerificationReportageImpl(this, pluginCoordinate, ideDescriptor.ideVersion, reporterSet)
  }

  override fun close() {
    reporterSetProvider.close()
  }

}
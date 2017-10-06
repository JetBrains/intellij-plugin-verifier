package com.jetbrains.pluginverifier.reporting.verification

import com.jetbrains.pluginverifier.misc.bytesToMegabytes
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.parameters.ide.IdeDescriptor
import com.jetbrains.pluginverifier.plugin.PluginCoordinate
import com.jetbrains.pluginverifier.reporting.message.MessageReporter
import com.jetbrains.pluginverifier.reporting.progress.ProgressReporter

class VerificationReportageImpl(private val messageReporters: List<MessageReporter>,
                                private val progressReporters: List<ProgressReporter>,
                                private val reporterSetProvider: ReporterSetProvider) : VerificationReportage {

  private var verifiedPlugins: Int = 0

  private var totalPlugins: Int = 0

  override fun logVerificationExecutorCreated(availableMemory: Long, availableCpu: Long, concurrencyLevel: Int) {
    reportMessage("Available memory: ${availableMemory.bytesToMegabytes()} Mb; Available CPU = $availableCpu; Concurrency level = $concurrencyLevel")
  }

  private fun reportMessage(message: String) {
    messageReporters.forEach { it.report(message) }
  }

  @Synchronized
  override fun logPluginVerificationFinished(pluginVerificationReportage: PluginVerificationReportage) {
    ++verifiedPlugins
    reportMessage("$verifiedPlugins of $totalPlugins plugins finished: ${pluginVerificationReportage.plugin} and #${pluginVerificationReportage.ideVersion}")
    pluginVerificationReportage.closeLogged()
    progressReporters.forEach { it.report(verifiedPlugins.toDouble() / totalPlugins) }
  }

  @Synchronized
  override fun createPluginLogger(pluginCoordinate: PluginCoordinate, ideDescriptor: IdeDescriptor): PluginVerificationReportage {
    totalPlugins++
    val reporterSet = reporterSetProvider.provide(pluginCoordinate, ideDescriptor.ideVersion)
    return PluginVerificationReportageImpl(this, pluginCoordinate, ideDescriptor.ideVersion, reporterSet)
  }

}
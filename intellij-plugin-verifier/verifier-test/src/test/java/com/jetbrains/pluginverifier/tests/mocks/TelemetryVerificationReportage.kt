package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.reporting.telemetry.TelemetryAggregator
import com.jetbrains.pluginverifier.repository.PluginInfo

class TelemetryVerificationReportage(private val delegate: MockPluginVerificationReportage = MockPluginVerificationReportage()) : PluginVerificationReportage by delegate {
  private val telemetryAggregator = TelemetryAggregator()

  override fun reportTelemetry(pluginInfo: PluginInfo, telemetry: PluginTelemetry) {
    telemetryAggregator.reportTelemetry(pluginInfo, telemetry)
  }

  operator fun get(plugin: PluginInfo): PluginTelemetry? {
    return telemetryAggregator[plugin]
  }
}

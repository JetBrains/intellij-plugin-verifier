package com.jetbrains.pluginverifier.reporting.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.formatDuration
import java.time.Duration

fun PluginTelemetry.toPlainString(): String {
  val telemetry = this
  return buildString {
    appendLine("Descriptor parsed in: ${parsingDuration.formatDuration()}")
    appendLine("Plugin size (bytes): $pluginSize")
    telemetry[PLUGIN_VERIFICATION_TIME]?.let {
      if (it is Duration) {
        appendLine("Verification time: ${it.formatDuration()}")
      }
    }
  }
}
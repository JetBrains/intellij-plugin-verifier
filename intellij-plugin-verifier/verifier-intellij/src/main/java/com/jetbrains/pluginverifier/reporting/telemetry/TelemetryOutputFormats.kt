package com.jetbrains.pluginverifier.reporting.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.formatDuration

fun PluginTelemetry.toPlainString(): String = buildString {
  appendLine("Plugin descriptor parsed in: ${parsingDuration.formatDuration()}")
  appendLine("Plugin size (bytes): ${pluginSize}")
}
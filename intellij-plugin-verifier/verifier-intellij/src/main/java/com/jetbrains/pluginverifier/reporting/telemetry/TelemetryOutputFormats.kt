package com.jetbrains.pluginverifier.reporting.telemetry

import com.jetbrains.plugin.structure.base.telemetry.*
import com.jetbrains.plugin.structure.base.utils.formatDuration
import java.time.Duration

fun PluginTelemetry.toPlainString(): String {
  val telemetry = this
  return buildString {
    appendLine(telemetry, PLUGIN_ID, "Plugin ID")
    appendLine(telemetry, PLUGIN_VERSION, "Plugin Version")
    appendLine("Descriptor parsed in: ${parsingDuration.formatDuration()}")
    appendLine("Descriptor parsed (raw ms): ${parsingDuration.toMillis()}")
    appendLine("Plugin size (bytes): $archiveFileSize")
    appendLine(telemetry, PLUGIN_VERIFIED_CLASSES_COUNT, "Verified classes in plugin artifact")
    telemetry[PLUGIN_VERIFICATION_TIME]?.let {
      if (it is Duration) {
        appendLine("Verification time: ${it.formatDuration()}")
        appendLine("Verification time (raw ms): ${it.toMillis()}")
      }
    }
  }
}

private fun StringBuilder.appendLine(telemetry: PluginTelemetry, key: String, keyDescription: String) {
  telemetry[key]?.let { value ->
    appendLine("$keyDescription: $value")
  }
}

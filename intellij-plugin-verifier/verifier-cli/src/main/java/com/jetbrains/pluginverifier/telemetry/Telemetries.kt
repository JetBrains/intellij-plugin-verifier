package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import java.io.File

fun readTelemetry(telemetryFile: File): MutablePluginTelemetry {
  val telemetry = MutablePluginTelemetry()
  telemetryFile.forEachLine { line ->
    val lineComponents = line.split(":").map { it.trim() }
    if (lineComponents.size == 2) {
      val key = lineComponents[0]
      val value = lineComponents[1]
      telemetry[key] = value
    }
  }
  return telemetry
}
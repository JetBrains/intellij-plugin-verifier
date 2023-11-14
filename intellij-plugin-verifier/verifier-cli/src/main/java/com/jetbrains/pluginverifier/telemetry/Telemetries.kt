package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.MutablePluginTelemetry
import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.base.utils.Bytes
import java.io.File
import java.time.Duration

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

fun PluginTelemetry.fromPlainStringTelemetry(): PluginTelemetry {
  return MutablePluginTelemetry().also { newTelemetry ->
    duration("Descriptor parsed (raw ms)") {
      newTelemetry.parsingDuration = it
    }
    duration("Verification time (raw ms)") {
      newTelemetry[PLUGIN_VERIFICATION_TIME] = it
    }
    bytes("Plugin size (bytes)") {
      newTelemetry.archiveFileSize = it
    }
  }
}

private inline fun PluginTelemetry.long(key: String, whenNotNull: (Long) -> Unit) {
  val value = this[key] ?: return
  if (value is Long) {
    whenNotNull(value)
  } else {
    val longValue = value.toString().toLongOrNull()
    if (longValue != null) {
      whenNotNull(longValue)
    }
  }
}

private inline fun PluginTelemetry.bytes(key: String, whenNotNull: (Bytes) -> Unit) {
  val value = this[key] ?: return
  if (value is Bytes) {
    whenNotNull(value)
  } else {
    long(key) {
      whenNotNull(it)
    }
  }
}

private inline fun PluginTelemetry.duration(key: String, whenNotNull: (Duration) -> Unit) {
  val value = this[key] ?: return
  if (value is Duration) {
    whenNotNull(value)
  } else {
    long(key) {
      whenNotNull(Duration.ofMillis(it))
    }
  }
}
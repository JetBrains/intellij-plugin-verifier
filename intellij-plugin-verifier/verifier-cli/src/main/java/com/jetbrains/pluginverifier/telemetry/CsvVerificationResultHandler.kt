package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.pluginverifier.telemetry.parsing.VerificationResultHandler
import java.io.File
import java.time.Duration

class CsvVerificationResultHandler(csvOutputFile: File) : VerificationResultHandler, AutoCloseable {
  private val SEPARATOR = ","

  private val columns = listOf("IDE version", "Plugin ID", "Plugin Version",
    "Plugin Size", "Verification Time",
    "Description Parsing Time")

  private val writer = csvOutputFile.bufferedWriter()

  override fun beforeVerificationResult() {
    writer.appendLine(columns.joinToString(SEPARATOR))
  }

  override fun onVerificationResult(verificationSpecificTelemetry: VerificationSpecificTelemetry) {
    val values = listOf(
      verificationSpecificTelemetry.ideVersion.toString(),
      verificationSpecificTelemetry.pluginIdAndVersion.pluginId,
      verificationSpecificTelemetry.pluginIdAndVersion.version,
      nullSafeGet { verificationSpecificTelemetry.telemetry.archiveFileSize.asString() },
      verificationSpecificTelemetry.telemetry[PLUGIN_VERIFICATION_TIME].asString(),
      nullSafeGet { verificationSpecificTelemetry.telemetry.parsingDuration.asString() }
    )

    writer.appendLine(values.joinToString(SEPARATOR))
  }

  private fun Any?.asString(): String {
    if (this == null) {
      return ""
    }
    return when (this) {
      is Duration -> toMillis().toString()
      else -> toString()
    }
  }

  // FIXME replace with runCatching
  private fun nullSafeGet(function: () -> String): String {
    return try {
      function()
    } catch (e: NullPointerException) {
      ""
    }
  }

  override fun close() {
    writer.close()
  }
}
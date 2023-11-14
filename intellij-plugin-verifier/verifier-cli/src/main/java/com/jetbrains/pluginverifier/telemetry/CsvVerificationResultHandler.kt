package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.pluginverifier.telemetry.parsing.VerificationResultHandler
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.bufferedWriter

class CsvVerificationResultHandler(csvOutput: Path) : VerificationResultHandler, AutoCloseable {
  private val SEPARATOR = ","

  private val columns = listOf("IDE version", "Plugin ID", "Plugin Version",
    "Plugin Size", "Verification Time",
    "Description Parsing Time")

  private val writer = csvOutput.bufferedWriter()

  override fun beforeVerificationResult() {
    writer.appendLine(columns.joinToString(SEPARATOR))
  }

  override fun onVerificationResult(verificationSpecificTelemetry: VerificationSpecificTelemetry) {
    val values = listOf(
      verificationSpecificTelemetry.ideVersion.toString(),
      verificationSpecificTelemetry.pluginIdAndVersion.pluginId,
      verificationSpecificTelemetry.pluginIdAndVersion.version,
      verificationSpecificTelemetry.telemetry.archiveFileSize.asString(),
      verificationSpecificTelemetry.telemetry[PLUGIN_VERIFICATION_TIME].asString(),
      verificationSpecificTelemetry.telemetry.parsingDuration.asString()
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

  override fun close() {
    writer.close()
  }
}
package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PLUGIN_VERIFICATION_TIME
import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.File
import java.nio.file.Path
import java.time.Duration

const val TELEMETRY_FILE = "telemetry.txt"

interface VerificationResultHandler {
  fun beforeVerificationResult() {}

  fun onVerificationResult(verificationResult: TelemetryAggregatorCli.LocatedTelemetry) {}

  fun afterVerificationResult() {}
}

class TelemetryAggregatorCli(private val verificationOutputDir: Path) {
  fun run(verificationResultHandler: VerificationResultHandler) {
    val ideDirs: Array<File> = verificationOutputDir.toFile().listFiles { child: File ->
      child.isDirectory && IdeVersion.createIdeVersionIfValid(child.name) != null
    } ?: emptyArray()

    verificationResultHandler.beforeVerificationResult()
    ideDirs.forEach { ideDir ->
      val pluginsDir = ideDir.pluginDir
      if (pluginsDir != null) {
        processPluginsDir(pluginsDir, IdeVersion.createIdeVersion(ideDir.name))
          .forEach {
            verificationResultHandler.onVerificationResult(it)
          }
      }
    }
    verificationResultHandler.afterVerificationResult()
  }

  private fun processPluginsDir(pluginDir: File, ideVersion: IdeVersion): Sequence<LocatedTelemetry> {
    return pluginDir.walkBottomUp().filter {
      it.name == TELEMETRY_FILE
    }.map { file ->
      file to file.path.removePrefix(pluginDir.path + File.separator)
    }
      .map { (file, name) ->
        val components = name.split(File.separator)
        parse(components, ideVersion, file)
      }
      .filterNotNull()

  }

  data class LocatedTelemetry(val ideVersion: IdeVersion, val pluginIdAndVersion: PluginIdAndVersion, val telemetry: PluginTelemetry)

  private fun parse(components: List<String>, ideVersion: IdeVersion, telemetryFile: File): LocatedTelemetry? {
    if (components.size != 3) {
      return null
    }
    val id = components[0]
    val version = components[1]
    val telemetry = readTelemetry(telemetryFile).fromPlainStringTelemetry()
    return LocatedTelemetry(ideVersion, PluginIdAndVersion(id, version), telemetry)
  }

  private val File.pluginDir
    get() = this.listFiles { child: File ->
      child.name == "plugins"
    }?.firstOrNull()
}

class CsvVerificationResultHandler(csvOutputFile: File) : VerificationResultHandler, AutoCloseable {
  private val SEPARATOR = ","

  private val columns = listOf("IDE version", "Plugin ID", "Plugin Version",
    "Plugin Size", "Verification Time",
    "Description Parsing Time")

  private val writer = csvOutputFile.bufferedWriter()

  override fun beforeVerificationResult() {
    writer.appendLine(columns.joinToString(SEPARATOR))
  }

  override fun onVerificationResult(verificationResult: TelemetryAggregatorCli.LocatedTelemetry) {
    val values = listOf(
      verificationResult.ideVersion.toString(),
      verificationResult.pluginIdAndVersion.pluginId,
      verificationResult.pluginIdAndVersion.version,
      nullSafeGet { verificationResult.telemetry.archiveFileSize.asString() },
      verificationResult.telemetry[PLUGIN_VERIFICATION_TIME].asString(),
      nullSafeGet { verificationResult.telemetry.parsingDuration.asString() }
    )

    writer.appendLine(values.joinToString(SEPARATOR))
  }

  private fun Any?.asString(): String {
    if (this == null) {
      return ""
    }
    return when (this) {
      is Duration -> this.toMillis().toString()
      else -> this.toString()
    }
  }

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

fun main(args: Array<String>) {
  require(args.isNotEmpty()) {
    "Path to the verification output directory must be set"
  }

  val path = Path.of(args.first())

  val verificationResultHandler = when {
    args.size > 1 -> {
      val csvOutputFile = File(args[1])
      CsvVerificationResultHandler(csvOutputFile)
    }

    else -> object : VerificationResultHandler {}
  }

  TelemetryAggregatorCli(path).run(verificationResultHandler)

  if (verificationResultHandler is AutoCloseable) {
    verificationResultHandler.close()
  }
}
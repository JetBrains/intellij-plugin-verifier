package com.jetbrains.pluginverifier.telemetry.parsing

import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.telemetry.VerificationSpecificTelemetry
import com.jetbrains.pluginverifier.telemetry.fromPlainStringTelemetry
import com.jetbrains.pluginverifier.telemetry.readTelemetry
import java.io.File
import java.nio.file.Path

const val TELEMETRY_FILE = "telemetry.txt"

/**
 * Walks the verification report directory to search particular plugin directories and files.
 *
 * @param verificationOutputDir the verification report directory path. E. g. `verification-2023-10-26 at 11.31.35`
 * as emitted by [com.jetbrains.pluginverifier.output.OutputOptions].
 *
 * @see [com.jetbrains.pluginverifier.output.OutputOptions.getTargetReportDirectory]
 */
class TargetReportDirectoryWalker(private val verificationOutputDir: Path) {
  fun walk(verificationResultHandler: VerificationResultHandler) {
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

  private fun processPluginsDir(pluginDir: File, ideVersion: IdeVersion): Sequence<VerificationSpecificTelemetry> {
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

  private fun parse(components: List<String>, ideVersion: IdeVersion, telemetryFile: File): VerificationSpecificTelemetry? {
    if (components.size != 3) {
      return null
    }
    val id = components[0]
    val version = components[1]
    val telemetry = readTelemetry(telemetryFile).fromPlainStringTelemetry()
    return VerificationSpecificTelemetry(ideVersion, PluginIdAndVersion(id, version), telemetry)
  }

  private val File.pluginDir
    get() = this.listFiles { child: File ->
      child.name == "plugins"
    }?.firstOrNull()
}
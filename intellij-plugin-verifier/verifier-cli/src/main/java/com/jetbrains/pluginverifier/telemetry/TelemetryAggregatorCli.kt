package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.File
import java.nio.file.Path

const val TELEMETRY_FILE = "telemetry.txt"

class TelemetryAggregatorCli(private val verificationOutputDir: Path) {
  fun run(verificationResultHandler: (VerificationDir) -> Unit = {}) {
    val ideDirs: Array<File> = verificationOutputDir.toFile().listFiles { child: File ->
      child.isDirectory && IdeVersion.createIdeVersionIfValid(child.name) != null
    } ?: emptyArray()

    ideDirs.forEach { ideDir ->
      val pluginsDir = ideDir.pluginDir
      if (pluginsDir != null) {
        processPluginsDir(pluginsDir, IdeVersion.createIdeVersion(ideDir.name))
          .forEach(verificationResultHandler)
      }
    }
  }

  private fun processPluginsDir(pluginDir: File, ideVersion: IdeVersion): Sequence<VerificationDir> {
    return pluginDir.walkBottomUp().filter {
      it.name == TELEMETRY_FILE
    }.take(20)
      .map { file ->
        file to file.path.removePrefix(pluginDir.path + File.separator)
      }
      .map { (file, name) ->
        val components = name.split(File.separator)
        parse(components, ideVersion, file)
      }
      .filterNotNull()

  }

  data class VerificationDir(val ideVersion: IdeVersion, val pluginIdAndVersion: PluginIdAndVersion, val telemetry: PluginTelemetry)

  private fun parse(components: List<String>, ideVersion: IdeVersion, telemetryFile: File): VerificationDir? {
    if (components.size != 3) {
      return null
    }
    val id = components[0]
    val version = components[1]
    val telemetry = readTelemetry(telemetryFile)
    return VerificationDir(ideVersion, PluginIdAndVersion(id, version), telemetry)
  }

  private val File.pluginDir
    get() = this.listFiles { child: File ->
      child.name == "plugins"
    }?.firstOrNull()
}

fun main(args: Array<String>) {
  require(args.isNotEmpty()) {
    "Path to the verification output directory must be set"
  }

  val path = Path.of(args.first())
  TelemetryAggregatorCli(path).run {
    println(it)
  }
}
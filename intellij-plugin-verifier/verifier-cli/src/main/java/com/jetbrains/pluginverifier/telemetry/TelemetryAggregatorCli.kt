package com.jetbrains.pluginverifier.telemetry

import com.jetbrains.plugin.structure.base.telemetry.PluginTelemetry
import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.io.File
import java.nio.file.Path

const val TELEMETRY_FILE = "telemetry.txt"

class TelemetryAggregatorCli(private val verificationOutputDir: Path) {
  fun run() {
    val ideDirs = verificationOutputDir.toFile().listFiles { child: File ->
      child.isDirectory && IdeVersion.createIdeVersionIfValid(child.name) != null
    }
    for (ideDir in ideDirs) {
      val pluginDir = ideDir.pluginDir
      if (pluginDir != null) {
        processPluginsDir(pluginDir, IdeVersion.createIdeVersion(ideDir.name))
      }
    }
  }

  private fun processPluginsDir(pluginDir: File, ideVersion: IdeVersion) {
    pluginDir.walkBottomUp().filter {
      it.name == TELEMETRY_FILE
    }.take(20)
      .map {
        it to it.path.removePrefix(pluginDir.path + File.separator)
      }
      .map { (file, name) ->
        val components = name.split(File.separator)
        file to parse(components, ideVersion, file)
      }
      .forEach { println(it) }
  }

  private data class VerificationDir(val ideVersion: IdeVersion, val pluginIdAndVersion: PluginIdAndVersion, val telemetry: PluginTelemetry)

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
  TelemetryAggregatorCli(path).run()
}
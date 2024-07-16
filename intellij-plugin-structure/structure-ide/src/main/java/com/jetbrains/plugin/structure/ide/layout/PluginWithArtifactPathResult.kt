package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.slf4j.Logger
import java.nio.file.Path

internal sealed class PluginWithArtifactPathResult(open val pluginArtifactPath: Path) {
  data class Success(override val pluginArtifactPath: Path, val plugin: IdePlugin) :
    PluginWithArtifactPathResult(pluginArtifactPath)

  data class Failure(override val pluginArtifactPath: Path, val pluginProblems: List<PluginProblem>) : PluginWithArtifactPathResult(pluginArtifactPath)

  companion object {
    internal fun logFailures(
      log: Logger,
      failures: List<PluginWithArtifactPathResult>,
      idePath: Path
    ) {
      if (failures.isNotEmpty()) {
        val failedPluginPaths = failures.map {
          idePath.relativize(it.pluginArtifactPath)
        }.joinToString(", ")
        log.atWarn().log("Following ${failures.size} plugins could not be created: $failedPluginPaths")
      }
    }
  }
}
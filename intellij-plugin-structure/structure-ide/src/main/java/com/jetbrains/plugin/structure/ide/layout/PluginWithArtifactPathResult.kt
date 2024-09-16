package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import org.slf4j.Logger
import java.nio.file.Path

internal sealed class PluginWithArtifactPathResult(open val pluginArtifactPath: Path) {
  data class Success(override val pluginArtifactPath: Path, val plugin: IdePlugin) :
    PluginWithArtifactPathResult(pluginArtifactPath)

  data class Failure(override val pluginArtifactPath: Path, val pluginProblems: List<PluginProblem>, val pluginName: String? = null) : PluginWithArtifactPathResult(pluginArtifactPath) {
    constructor(pluginArtifactPath: Path, pluginName: String?, vararg pluginProblems: PluginProblem) : this(pluginArtifactPath, pluginProblems.toList(), pluginName)
  }

  companion object {
    internal fun logFailures(
      log: Logger,
      failures: List<PluginWithArtifactPathResult>,
      idePath: Path
    ) {
      if (failures.isNotEmpty()) {
        val failedPluginPaths = getFailedPluginPaths(failures, idePath)
        log.atWarn().log("Following ${failures.size} plugins could not be created: $failedPluginPaths")
        if (log.isDebugEnabled) {
          buildString {
            failures
              .filterIsInstance<Failure>()
              .forEach { (pluginArtifactPath, problems, pluginName) ->
                append("Unable to load")
                val pluginArtifactRelativePath = idePath.relativize(pluginArtifactPath)
                if (pluginName != null) {
                  append(" '$pluginName' from '$pluginArtifactRelativePath")
                } else {
                  append("'$pluginArtifactRelativePath'")
                }.append(": ")
                append(problems.joinToString("\n* ", "\n* "))
              }
          }.let { log.debug(it) }
        }
      }
    }

    private fun getFailedPluginPaths(
      failures: List<PluginWithArtifactPathResult>,
      idePath: Path
    ) = failures.map {
      if (it is Failure && it.pluginName != null) {
        "${it.pluginName}'" + " from '" + idePath.relativize(it.pluginArtifactPath) + "'"
      } else {
        idePath.relativize(it.pluginArtifactPath)
      }
    }.joinToString(", ")
  }
}
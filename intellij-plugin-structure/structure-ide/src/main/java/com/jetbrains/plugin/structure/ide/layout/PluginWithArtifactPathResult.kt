package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

internal sealed class PluginWithArtifactPathResult(open val pluginArtifactPath: Path) {
  data class Success(override val pluginArtifactPath: Path, val plugin: IdePlugin) :
    PluginWithArtifactPathResult(pluginArtifactPath)

  data class Failure(override val pluginArtifactPath: Path) : PluginWithArtifactPathResult(pluginArtifactPath)
}
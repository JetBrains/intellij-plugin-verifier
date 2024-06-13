package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

fun interface ModuleLoader {
  fun load(
    pluginArtifactPath: Path,
    descriptorName: String,
    resourceResolver: ResourceResolver,
    ideVersion: IdeVersion
  ): ProductInfoBasedIdeManager.PluginWithArtifactPathResult
}
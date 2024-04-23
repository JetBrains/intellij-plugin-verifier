package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import java.nio.file.Path

internal fun interface PluginLoader {
  fun load(
    pluginFile: Path,
    descriptorPath: String,
    validateDescriptor: Boolean,
    resourceResolver: ResourceResolver,
    parentPlugin: PluginCreator?,
    problemResolver: PluginCreationResultResolver
  ): PluginCreator
}
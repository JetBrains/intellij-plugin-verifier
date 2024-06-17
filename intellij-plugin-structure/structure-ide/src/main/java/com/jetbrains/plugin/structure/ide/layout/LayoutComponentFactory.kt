package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

internal interface LayoutComponentFactory<T : LayoutComponent> {
  fun read(
    layoutComponent: T,
    idePath: Path,
    ideVersion: IdeVersion,
    resourceResolver: ResourceResolver,
    moduleManager: BundledModulesManager
  ): PluginWithArtifactPathResult?
}
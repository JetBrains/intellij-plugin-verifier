package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.problems.MissedFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.getCommonParentDirectory
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent.Plugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginFileNotFoundException
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import java.nio.file.Path

internal class PluginFactory(private val pluginLoader: LayoutComponentLoader) : LayoutComponentFactory<Plugin> {
  override fun read(
    layoutComponent: Plugin,
    idePath: Path,
    ideVersion: IdeVersion,
    resourceResolver: ResourceResolver,
    moduleManager: BundledModulesManager
  ): PluginWithArtifactPathResult? {
    val relativePluginDir = getRelativePluginDirectory(layoutComponent) ?: return null
    val pluginDir = idePath.resolve(relativePluginDir)
    return try {
      pluginLoader.load(pluginArtifactPath = pluginDir, PLUGIN_XML, resourceResolver, ideVersion, layoutComponent.name)
    } catch (e: PluginFileNotFoundException) {
      PluginWithArtifactPathResult.Failure(pluginDir, layoutComponent.name, MissedFile(pluginDir.toString()))
    }
  }

  private fun getRelativePluginDirectory(pluginComponent: Plugin): Path? {
    val commonParent = getCommonParentDirectory(pluginComponent.getClasspath()) ?: return null
    return if (commonParent.simpleName == "lib") commonParent.parent else commonParent
  }
}
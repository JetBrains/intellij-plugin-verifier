package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager
import com.jetbrains.plugin.structure.ide.getCommonParentDirectory
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent.Plugin
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import java.nio.file.Path

class PluginFactory(private val pluginLoader: LayoutComponentLoader) : LayoutComponentFactory<Plugin> {
  override fun read(
    layoutComponent: Plugin,
    idePath: Path,
    ideVersion: IdeVersion,
    resourceResolver: ResourceResolver,
    moduleManager: BundledModulesManager
  ): ProductInfoBasedIdeManager.PluginWithArtifactPathResult? {

    return getRelativePluginDirectory(layoutComponent)
      ?.let { idePath.resolve(it) }
      ?.let { pluginLoader.load(pluginArtifactPath = it, PLUGIN_XML, resourceResolver, ideVersion) }
  }

  private fun getRelativePluginDirectory(pluginComponent: Plugin): Path? {
    val commonParent = getCommonParentDirectory(pluginComponent.getClasspath()) ?: return null
    return if (commonParent.simpleName == "lib") commonParent.parent else commonParent
  }
}
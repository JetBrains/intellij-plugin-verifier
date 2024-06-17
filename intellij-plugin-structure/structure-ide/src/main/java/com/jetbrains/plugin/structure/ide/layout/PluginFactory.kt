package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager
import com.jetbrains.plugin.structure.ide.getCommonParentDirectory
import com.jetbrains.plugin.structure.intellij.platform.BundledModulesManager
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import java.nio.file.Path

class PluginFactory(private val pluginLoader: LayoutComponentLoader) {
  fun read(
    pluginComponent: LayoutComponent.Plugin,
    idePath: Path,
    ideVersion: IdeVersion,
    resourceResolver: ResourceResolver,
    @Suppress("UNUSED_PARAMETER") moduleManager: BundledModulesManager
  ): ProductInfoBasedIdeManager.PluginWithArtifactPathResult? {

    return getRelativePluginDirectory(pluginComponent)
      ?.let { idePath.resolve(it) }
      ?.let { pluginLoader.load(pluginArtifactPath = it, PLUGIN_XML, resourceResolver, ideVersion) }
  }

  private fun getRelativePluginDirectory(pluginComponent: LayoutComponent.Plugin): Path? {
    val commonParent = getCommonParentDirectory(pluginComponent.getClasspath()) ?: return null
    return if (commonParent.simpleName == "lib") commonParent.parent else commonParent
  }
}
package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.BundledPluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import java.nio.file.Path

/**
 * Provides plugin details with explicit support for bundled plugins that are provided by the Platform.
 *
 * Non-bundled plugins are handled by the delegate [PluginDetailsProviderImpl].
 */
class DefaultPluginDetailsProvider(extractDirectory: Path) : AbstractPluginDetailsProvider(extractDirectory) {
  private val nonBundledPluginDetailsProvider: PluginDetailsProviderImpl = PluginDetailsProviderImpl(extractDirectory)

  override fun readPluginClasses(pluginInfo: PluginInfo, idePlugin: IdePlugin): IdePluginClassesLocations {
    return if (pluginInfo is BundledPluginInfo) {
      BundledPluginClassesFinder.findPluginClasses(idePlugin, additionalKeys = listOf(CompileServerExtensionKey))
    } else {
      nonBundledPluginDetailsProvider.readPluginClasses(pluginInfo, idePlugin)
    }
  }
}
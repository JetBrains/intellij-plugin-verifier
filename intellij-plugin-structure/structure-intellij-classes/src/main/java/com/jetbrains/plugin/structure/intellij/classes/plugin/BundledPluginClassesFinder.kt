package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.classes.IdeFileOrigin
import com.jetbrains.plugin.structure.intellij.classes.locator.FileOriginProvider
import com.jetbrains.plugin.structure.intellij.classes.locator.JarPluginLocator
import com.jetbrains.plugin.structure.intellij.classes.locator.LibDirectoryLocator
import com.jetbrains.plugin.structure.intellij.classes.locator.LibModulesDirectoryLocator
import com.jetbrains.plugin.structure.intellij.classes.locator.LocationKey
import com.jetbrains.plugin.structure.intellij.classes.locator.MODULES_DIR
import com.jetbrains.plugin.structure.intellij.classes.locator.NotAModulesDirectoryFilter
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

class BundledPluginClassesFinder {
  companion object {
    val LOCATION_KEYS = listOf(BundledPluginJarKey, BundledPluginDirectoryKey, BundledPluginLibModulesDirectoryKey)

    fun findPluginClasses(
      idePlugin: IdePlugin,
      additionalKeys: List<LocationKey> = emptyList(),
      searchContext: ClassSearchContext
    ): IdePluginClassesLocations {
      return IdePluginClassesFinder.fullyFindPluginClassesInExplicitLocations(idePlugin, LOCATION_KEYS + additionalKeys, searchContext)
    }
  }

  object BundledPluginJarKey : LocationKey {
    override val name: String = "Bundled Plugin JAR"
    override fun getLocator(readMode: Resolver.ReadMode) = JarPluginLocator(readMode, BundledPluginOriginator)
  }

  object BundledPluginDirectoryKey : LocationKey {
    override val name: String = "Bundled Plugin Directory"
    override fun getLocator(readMode: Resolver.ReadMode) =
      LibDirectoryLocator(readMode, BundledPluginOriginator, NotAModulesDirectoryFilter)
  }

  object BundledPluginOriginator : FileOriginProvider {
    override fun getFileOrigin(idePlugin: IdePlugin, pluginFile: Path): FileOrigin {
      return IdeFileOrigin.BundledPlugin(pluginFile, idePlugin)
    }
  }

  object BundledPluginLibModulesDirectoryKey : LocationKey {
    override val name: String = "Bundled Plugin lib/$MODULES_DIR directory"

    override fun getLocator(readMode: Resolver.ReadMode) = LibModulesDirectoryLocator(readMode)
  }
}
package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildJarOrZipFileResolvers
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

/**
 * Locates classes from `lib/modules` directory.
 *
 * Such classes correspond to V2 plugin modules with module-level libraries packaged into separate JARs.
 */
class LibModulesDirectoryLocator(
  private val readMode: Resolver.ReadMode,
  private val fileOriginProvider: FileOriginProvider = LibModulesDirectoryOriginProvider
) : ClassesLocator {

  override val locationKey = LibModulesDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: Path): List<Resolver> {
    val modulesDir = pluginFile.resolve(LIB_DIR).resolve(MODULES_DIR)
    if (!modulesDir.isDirectory) {
      return emptyList()
    }

    val origin = fileOriginProvider.getFileOrigin(idePlugin, pluginFile)
    val jarsOrZips = modulesDir.listJars()

    return buildJarOrZipFileResolvers(jarsOrZips, readMode, origin)
  }
}

object LibModulesDirectoryKey : LocationKey {
  override val name: String = "$LIB_DIR/$MODULES_DIR directory"

  override fun getLocator(readMode: Resolver.ReadMode) = LibModulesDirectoryLocator(readMode)
}

object LibModulesDirectoryOriginProvider : FileOriginProvider {
  override fun getFileOrigin(idePlugin: IdePlugin, pluginFile: Path) = PluginFileOrigin.LibDirectory(idePlugin)
}
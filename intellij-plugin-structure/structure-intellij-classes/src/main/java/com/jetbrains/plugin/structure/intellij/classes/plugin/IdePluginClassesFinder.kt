/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.ClassesDirectoryKey
import com.jetbrains.plugin.structure.intellij.classes.locator.JarPluginKey
import com.jetbrains.plugin.structure.intellij.classes.locator.LibDirectoryKey
import com.jetbrains.plugin.structure.intellij.classes.locator.LibModulesDirectoryKey
import com.jetbrains.plugin.structure.intellij.classes.locator.LocationKey
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.caches.PluginResourceCache
import com.jetbrains.plugin.structure.intellij.resources.ZipPluginResource
import com.jetbrains.plugin.structure.intellij.resources.ZipPluginResource.Companion.matches
import java.io.Closeable
import java.io.IOException
import java.nio.file.Path

/**
 * Discovers all classes that belong to the plugin.
 *
 * During this process, ZIP-packaged plugins are automatically decompressed to a dedicated [extractDirectory]
 * to enable the discovery of nested JAR files and directories.
 * @param idePlugin a descriptor of the plugin
 * @param extractDirectory a path that is used to deflate compressed plugins (ZIPs and JARs)
 * @param readMode a suggested level of granularity used to discover classes
 * @param locatorKeys a default set of locations that are available for class discovery
 */
class IdePluginClassesFinder private constructor(
  private val idePlugin: IdePlugin,
  private val extractDirectory: Path,
  private val readMode: Resolver.ReadMode,
  private val locatorKeys: List<LocationKey>,
  private val pluginResourceCache: PluginResourceCache
) {

  private fun findPluginClasses(): IdePluginClassesLocations {
    val pluginFile = idePlugin.originalFile
    if (pluginFile == null) {
      return IdePluginClassesLocations(idePlugin, Closeable { /* Nothing to close */ }, emptyMap())
    } else if (!pluginFile.exists()) {
      throw IllegalArgumentException("Plugin file doesn't exist $pluginFile")
    } else if (!pluginFile.isDirectory && !pluginFile.isJar() && !pluginFile.isZip()) {
      throw IllegalArgumentException("Incorrect plugin file type $pluginFile: expected a directory, a .zip or a .jar archive")
    }

    return if (pluginFile.isZip()) {
      findInZip(pluginFile)
    } else {
      val locations = findLocations(pluginFile)
      IdePluginClassesLocations(idePlugin, Closeable { /* Nothing to delete */ }, locations)
    }
  }

  private fun findInZip(pluginZip: Path): IdePluginClassesLocations {
    val cachedResult = pluginResourceCache.findFirst(pluginZip.matches())
    return when (cachedResult) {
      is PluginResourceCache.Result.Found ->
        cachedResult.pluginResource.let { it ->
          IdePluginClassesLocations(
            idePlugin,
            allocatedResource = it,
            findLocations(it.extractedPluginPath)
          )
        }

      is PluginResourceCache.Result.NotFound -> {
        extractAndGetClasses(pluginZip).let { (extractedPluginPath, locations) ->
          pluginResourceCache += ZipPluginResource.of(pluginZip, extractedPluginPath, idePlugin)
          locations
        }
      }
    }
  }

  private fun Path.matches() = { zipResource: ZipPluginResource -> zipResource.matches(this, idePlugin) }

  @Throws(IOException::class)
  private fun extractAndGetClasses(pluginZipPath: Path): Pair<Path, IdePluginClassesLocations> {
    return when (val extractorResult = PluginExtractor.extractPlugin(pluginZipPath, extractDirectory)) {
      is ExtractorResult.Success -> {
        extractorResult.extractedPlugin.closeOnException {
          val locations = findLocations(it.pluginFile)
          it.pluginFile to IdePluginClassesLocations(idePlugin, it, locations)
        }
      }

      is ExtractorResult.Fail -> throw IOException(extractorResult.pluginProblem.message)
    }
  }

  private fun findLocations(pluginFile: Path): Map<LocationKey, List<Resolver>> {
    val locations = hashMapOf<LocationKey, List<Resolver>>()
    try {
      for (locatorKey in locatorKeys) {
        checkIfInterrupted()
        val resolvers = locatorKey.getLocator(readMode).findClasses(idePlugin, pluginFile)
        locations[locatorKey] = resolvers
      }
    } catch (e: Throwable) {
      for (resolvers in locations.values) {
        resolvers.forEach { it.closeLogged() }
      }
      throw e
    }
    return locations
  }

  companion object {

    val MAIN_CLASSES_KEYS = listOf(JarPluginKey, ClassesDirectoryKey, LibDirectoryKey, LibModulesDirectoryKey)

    fun findPluginClasses(
      idePlugin: IdePlugin,
      additionalKeys: List<LocationKey> = emptyList()
    ): IdePluginClassesLocations =
      find(idePlugin, MAIN_CLASSES_KEYS + additionalKeys, Resolver.ReadMode.FULL, ClassSearchContext())

    fun findPluginClasses(
      idePlugin: IdePlugin,
      additionalKeys: List<LocationKey> = emptyList(),
      searchContext: ClassSearchContext
    ): IdePluginClassesLocations =
      find(idePlugin, MAIN_CLASSES_KEYS + additionalKeys, Resolver.ReadMode.FULL, searchContext)

    fun fullyFindPluginClassesInExplicitLocations(
      idePlugin: IdePlugin,
      locations: List<LocationKey>,
      searchContext: ClassSearchContext
    ): IdePluginClassesLocations =
      find(idePlugin, locations, Resolver.ReadMode.FULL, searchContext)

    private fun find(
      idePlugin: IdePlugin,
      explicitLocations: List<LocationKey>,
      readMode: Resolver.ReadMode,
      searchContext: ClassSearchContext
    ): IdePluginClassesLocations =
      IdePluginClassesFinder(
        idePlugin,
        searchContext.extractDirectory,
        readMode,
        explicitLocations,
        searchContext.pluginCache
      ).findPluginClasses()
  }
}

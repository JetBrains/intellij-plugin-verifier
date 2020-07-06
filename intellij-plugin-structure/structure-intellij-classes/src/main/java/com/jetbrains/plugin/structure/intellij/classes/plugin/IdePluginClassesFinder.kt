/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.utils.*
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.ClassesDirectoryKey
import com.jetbrains.plugin.structure.intellij.classes.locator.JarPluginKey
import com.jetbrains.plugin.structure.intellij.classes.locator.LibDirectoryKey
import com.jetbrains.plugin.structure.intellij.classes.locator.LocationKey
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorResult
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.Closeable
import java.io.File
import java.io.IOException

class IdePluginClassesFinder private constructor(
  private val idePlugin: IdePlugin,
  private val extractDirectory: File,
  private val readMode: Resolver.ReadMode,
  private val locatorKeys: List<LocationKey>
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

  private fun findInZip(pluginZip: File): IdePluginClassesLocations {
    return when (val extractorResult = PluginExtractor.extractPlugin(pluginZip.inputStream(), extractDirectory)) {
      is ExtractorResult.Success -> {
        extractorResult.extractedPlugin.closeOnException {
          val locations = findLocations(it.pluginFile)
          IdePluginClassesLocations(idePlugin, it, locations)
        }
      }
      is ExtractorResult.Fail -> throw IOException(extractorResult.pluginProblem.message)
    }
  }

  private fun findLocations(pluginFile: File): Map<LocationKey, List<Resolver>> {
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

    val MAIN_CLASSES_KEYS = listOf(JarPluginKey, ClassesDirectoryKey, LibDirectoryKey)

    fun findPluginClasses(
      idePlugin: IdePlugin,
      additionalKeys: List<LocationKey> = emptyList()
    ): IdePluginClassesLocations =
      findPluginClasses(idePlugin, Resolver.ReadMode.FULL, additionalKeys)

    fun findPluginClasses(
      idePlugin: IdePlugin,
      readMode: Resolver.ReadMode = Resolver.ReadMode.FULL,
      additionalKeys: List<LocationKey> = emptyList()
    ): IdePluginClassesLocations {
      val extractDirectory = Settings.EXTRACT_DIRECTORY.getAsFile().createDir()
      return findPluginClasses(idePlugin, extractDirectory, readMode, additionalKeys)
    }

    fun findPluginClasses(
      idePlugin: IdePlugin,
      extractDirectory: File,
      readMode: Resolver.ReadMode = Resolver.ReadMode.FULL,
      additionalKeys: List<LocationKey> = emptyList()
    ): IdePluginClassesLocations = IdePluginClassesFinder(
      idePlugin,
      extractDirectory,
      readMode,
      MAIN_CLASSES_KEYS + additionalKeys
    ).findPluginClasses()
  }

}

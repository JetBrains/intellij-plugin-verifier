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
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
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
    val extractorResult = PluginExtractor.extractPlugin(pluginZip, extractDirectory)
    return when (extractorResult) {
      is ExtractorResult.Success -> {
        extractorResult.extractedPlugin.closeOnException {
          val locations = findLocations(it.pluginFile)
          IdePluginClassesLocations(idePlugin, it, locations)
        }
      }
      is ExtractorResult.Fail -> throw IOException(extractorResult.pluginProblem.message)
    }
  }

  private fun findLocations(pluginFile: File): Map<LocationKey, Resolver> {
    val locations = hashMapOf<LocationKey, Resolver>()
    try {
      for (locatorKey in locatorKeys) {
        checkIfInterrupted()
        val resolver = locatorKey.getLocator(readMode).findClasses(idePlugin, pluginFile)
        if (resolver != null) {
          locations[locatorKey] = resolver
        }
      }
    } catch (e: Throwable) {
      locations.values.forEach { it.closeLogged() }
      throw e
    }
    return locations
  }

  companion object {

    val MAIN_CLASSES_KEYS = listOf(JarPluginKey, ClassesDirectoryKey, LibDirectoryKey)

    fun findPluginClasses(idePlugin: IdePlugin, additionalKeys: List<LocationKey> = emptyList()): IdePluginClassesLocations =
        findPluginClasses(idePlugin, Resolver.ReadMode.FULL, additionalKeys)

    fun findPluginClasses(
        idePlugin: IdePlugin,
        readMode: Resolver.ReadMode = Resolver.ReadMode.FULL,
        additionalKeys: List<LocationKey> = emptyList()
    ): IdePluginClassesLocations {
      val extractDirectory = if (idePlugin is IdePluginImpl) {
        idePlugin.extractDirectory
      } else {
        Settings.EXTRACT_DIRECTORY.getAsFile()
      }
      return findPluginClasses(idePlugin, extractDirectory!!, readMode, additionalKeys)
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

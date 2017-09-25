package com.jetbrains.plugin.structure.intellij.classes.plugin

import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.utils.FileUtil
import com.jetbrains.plugin.structure.base.utils.FileUtil.isJar
import com.jetbrains.plugin.structure.base.utils.FileUtil.isZip
import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.*
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorFail
import com.jetbrains.plugin.structure.intellij.extractor.ExtractorSuccess
import com.jetbrains.plugin.structure.intellij.extractor.PluginExtractor
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
class IdePluginClassesFinder private constructor(private val idePlugin: IdePlugin,
                                                 private val extractDirectory: File,
                                                 private val locators: List<IdePluginClassesLocator>) {

  private fun createLocationsContainer(): ClassLocationsContainer {
    val pluginFile = idePlugin.originalFile
    if (pluginFile == null) {
      throw IllegalArgumentException("Class path cannot be created for optional plugin descriptor $idePlugin")
    } else if (!pluginFile.exists()) {
      throw IllegalArgumentException("Plugin file doesn't exist " + pluginFile)
    } else if (!pluginFile.isDirectory && !isJar(pluginFile) && !isZip(pluginFile)) {
      throw IllegalArgumentException("Incorrect plugin file type $pluginFile: expected a directory, a .zip or a .jar archive")
    }

    return if (FileUtil.isZip(pluginFile)) {
      createClassPathFromZip(pluginFile)
    } else {
      val classPaths = createClassPaths(pluginFile)
      ClassLocationsContainer(Closeable { /* Nothing to delete */ }, classPaths)
    }
  }

  private fun createClassPathFromZip(pluginDir: File): ClassLocationsContainer {
    val extractorResult = PluginExtractor.extractPlugin(pluginDir, extractDirectory)
    return when (extractorResult) {
      is ExtractorSuccess -> {
        val extractedPlugin = extractorResult.extractedPlugin
        val classPaths = createClassPaths(extractedPlugin.pluginFile)
        ClassLocationsContainer(extractedPlugin, classPaths)
      }
      is ExtractorFail -> throw IOException(extractorResult.pluginProblem.message)
    }
  }

  private fun createClassPaths(pluginFile: File): Map<LocationKey, Resolver> {
    val classesLocations = hashMapOf<LocationKey, Resolver>()
    try {
      for (locator in locators) {
        val resolver = locator.findClasses(idePlugin, pluginFile)
        if (resolver != null) {
          classesLocations[locator.locationKey] = resolver
        }
      }
    } catch (e: Throwable) {
      classesLocations.values.forEach { it.closeLogged() }
      throw e
    }
    return classesLocations
  }

  companion object {

    private val DEFAULT_LOCATORS = listOf(JarPluginLocator(), ClassesDirectoryLocator(), LibDirectoryLocator())

    fun createLocationsContainer(idePlugin: IdePlugin, additionalLocators: List<IdePluginClassesLocator>): ClassLocationsContainer {
      val extractDirectory = if (idePlugin is IdePluginImpl) idePlugin.extractDirectory else Settings.EXTRACT_DIRECTORY.getAsFile()
      return createLocationsContainer(idePlugin, extractDirectory, additionalLocators)
    }

    fun createLocationsContainer(idePlugin: IdePlugin, extractDirectory: File, additionalLocators: List<IdePluginClassesLocator>): ClassLocationsContainer {
      val locators = DEFAULT_LOCATORS + additionalLocators
      return IdePluginClassesFinder(idePlugin, extractDirectory, locators).createLocationsContainer()
    }
  }

}

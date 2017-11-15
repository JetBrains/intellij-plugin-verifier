package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.File

class PluginDetailsProviderImpl(private val extractDirectory: File) : PluginDetailsProvider {
  override fun provideDetailsByExistingPlugins(plugin: IdePlugin): PluginDetails {
    val originalFile = plugin.originalFile
    return if (originalFile != null) {
      val pluginClassesLocations = try {
        plugin.findPluginClasses()
      } catch (e: Exception) {
        return PluginDetails.BadPlugin(listOf(UnableToReadPluginClassFilesProblem(e)))
      }
      PluginDetails.ByFileLock(plugin, pluginClassesLocations, emptyList(), IdleFileLock(originalFile))
    } else {
      PluginDetails.NotFound("Plugin classes are not found")
    }
  }

  override fun providePluginDetails(pluginCoordinate: PluginCoordinate): PluginDetails {
    val pluginFileFindResult = pluginCoordinate.fileFinder.findPluginFile()
    return when (pluginFileFindResult) {
      is PluginFileFinder.Result.Found -> createPluginDetailsByFileLock(pluginFileFindResult.pluginFileLock)
      is PluginFileFinder.Result.FailedToDownload -> PluginDetails.FailedToDownload(pluginFileFindResult.reason)
      is PluginFileFinder.Result.NotFound -> PluginDetails.NotFound(pluginFileFindResult.reason)
    }
  }

  private fun createPluginDetailsByFileLock(pluginFileLock: FileLock): PluginDetails {
    try {
      val pluginFile = pluginFileLock.file
      val creationResult = IdePluginManager.createManager(extractDirectory).createPlugin(pluginFile)
      if (creationResult is PluginCreationSuccess<IdePlugin>) {
        val pluginClassesLocations = try {
          creationResult.plugin.findPluginClasses()
        } catch (e: Exception) {
          return PluginDetails.BadPlugin(listOf(UnableToReadPluginClassFilesProblem(e)))
        }
        return PluginDetails.ByFileLock(creationResult.plugin, pluginClassesLocations, creationResult.warnings, pluginFileLock)
      } else {
        pluginFileLock.close()
        return PluginDetails.BadPlugin((creationResult as PluginCreationFail<*>).errorsAndWarnings)
      }
    } catch (e: Throwable) {
      pluginFileLock.closeLogged()
      throw e
    }
  }

  private fun IdePlugin.findPluginClasses(): IdePluginClassesLocations =
      IdePluginClassesFinder.findPluginClasses(this, additionalKeys = listOf(CompileServerExtensionKey))

}

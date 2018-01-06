package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
import java.nio.file.Path

class PluginDetailsProviderImpl(private val extractDirectory: Path) : PluginDetailsProvider {
  override fun provideDetailsByExistingPlugins(plugin: IdePlugin): PluginDetails {
    val originalFile = plugin.originalFile
    return if (originalFile != null) {
      val pluginClassesLocations = try {
        plugin.findPluginClasses()
      } catch (e: Exception) {
        return PluginDetails.BadPlugin(listOf(UnableToReadPluginClassFilesProblem(e)))
      }
      PluginDetails.ByFileLock(plugin, pluginClassesLocations, emptyList(), IdleFileLock(originalFile.toPath()))
    } else {
      PluginDetails.NotFound("Plugin classes are not found")
    }
  }

  override fun providePluginDetails(pluginInfo: PluginInfo): PluginDetails {
    return with(pluginInfo.pluginRepository.downloadPluginFile(pluginInfo)) {
      when (this) {
        is FileRepositoryResult.Found -> createPluginDetailsByFileLock(lockedFile)
        is FileRepositoryResult.NotFound -> PluginDetails.FailedToDownload(reason)
        is FileRepositoryResult.Failed -> PluginDetails.NotFound(reason)
      }
    }
  }

  private fun createPluginDetailsByFileLock(pluginFileLock: FileLock): PluginDetails {
    try {
      val pluginFile = pluginFileLock.file
      val creationResult = IdePluginManager.createManager(extractDirectory.toFile()).createPlugin(pluginFile.toFile())
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

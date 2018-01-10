package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.listFiles
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.nio.file.Path

/**
 * Main implementation of the [PluginDetailsProvider] that
 * uses the [extractDirectory] for extracting the `.zip`-ped plugins.
 */
class PluginDetailsProviderImpl(private val extractDirectory: Path) : PluginDetailsProvider {

  init {
    for (oldPlugin in extractDirectory.listFiles()) {
      oldPlugin.deleteLogged()
    }
  }

  override fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock): PluginDetailsProvider.Result {
    pluginFileLock.closeOnException {
      val pluginFile = pluginFileLock.file
      val pluginCreationResult = IdePluginManager.createManager(extractDirectory.toFile()).createPlugin(pluginFile.toFile())
      if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
        val pluginClassesLocations = try {
          pluginCreationResult.plugin.findPluginClasses()
        } catch (e: Exception) {
          return PluginDetailsProvider.Result.InvalidPlugin(listOf(UnableToReadPluginClassFilesProblem(e)))
        }
        return PluginDetailsProvider.Result.Provided(
            PluginDetails(
                pluginInfo,
                pluginCreationResult.plugin,
                pluginCreationResult.warnings,
                pluginClassesLocations,
                pluginFileLock
            )
        )
      } else {
        pluginFileLock.closeLogged()
        return PluginDetailsProvider.Result.InvalidPlugin((pluginCreationResult as PluginCreationFail<*>).errorsAndWarnings)
      }
    }
  }

  private fun IdePlugin.findPluginClasses() = IdePluginClassesFinder.findPluginClasses(this, additionalKeys = listOf(CompileServerExtensionKey))

}

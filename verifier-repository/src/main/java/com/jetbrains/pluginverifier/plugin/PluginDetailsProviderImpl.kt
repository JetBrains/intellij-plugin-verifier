package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.nio.file.Path

/**
 * Main implementation of the [PluginDetailsProvider] that
 * uses the [extractDirectory] for extracting the `.zip`-ped plugins.
 */
class PluginDetailsProviderImpl(private val extractDirectory: Path) : PluginDetailsProvider {

  override fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock): PluginDetailsProvider.Result {
    if (pluginInfo is BundledPluginInfo) {
      return findPluginClasses(pluginInfo, pluginFileLock, pluginInfo.idePlugin, emptyList())
    }

    pluginFileLock.closeOnException {
      val pluginFile = pluginFileLock.file
      val pluginCreationResult = IdePluginManager.createManager(extractDirectory.toFile()).createPlugin(pluginFile.toFile())
      return if (pluginCreationResult is PluginCreationSuccess<IdePlugin>) {
        findPluginClasses(pluginInfo, pluginFileLock, pluginCreationResult.plugin, pluginCreationResult.warnings)
      } else {
        pluginFileLock.closeLogged()
        PluginDetailsProvider.Result.InvalidPlugin((pluginCreationResult as PluginCreationFail<*>).errorsAndWarnings)
      }
    }
  }

  private fun findPluginClasses(pluginInfo: PluginInfo,
                                pluginFileLock: FileLock,
                                idePlugin: IdePlugin,
                                warnings: List<PluginProblem>): PluginDetailsProvider.Result {
    val pluginClassesLocations = try {
      idePlugin.findPluginClasses()
    } catch (e: Exception) {
      return PluginDetailsProvider.Result.InvalidPlugin(listOf(UnableToReadPluginClassFilesProblem(e)))
    }
    return PluginDetailsProvider.Result.Provided(
        PluginDetails(
            pluginInfo,
            idePlugin,
            warnings,
            pluginClassesLocations,
            pluginFileLock
        )
    )
  }

  private fun IdePlugin.findPluginClasses() = IdePluginClassesFinder.findPluginClasses(this, additionalKeys = listOf(CompileServerExtensionKey))

}

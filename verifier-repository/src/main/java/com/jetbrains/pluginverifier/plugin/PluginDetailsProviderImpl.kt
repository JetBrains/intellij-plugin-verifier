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
import org.slf4j.LoggerFactory
import java.nio.file.Path

/**
 * Main implementation of the [PluginDetailsProvider] that
 * uses the [extractDirectory] for extracting the `.zip`-ped plugins.
 */
class PluginDetailsProviderImpl(private val extractDirectory: Path) : PluginDetailsProvider {
  companion object {
    private val LOG = LoggerFactory.getLogger(PluginDetailsProviderImpl::class.java)
  }

  private val idePluginManager = IdePluginManager.createManager(extractDirectory.toFile())

  override fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock) =
      pluginFileLock.closeOnException {
        if (pluginInfo is BundledPluginInfo) {
          findPluginClasses(pluginInfo.idePlugin!!, emptyList(), pluginFileLock)
        } else {
          createPluginDetails(pluginFileLock.file, pluginFileLock)
        }
      }

  override fun providePluginDetails(pluginFile: Path) = createPluginDetails(pluginFile, null)

  private fun createPluginDetails(pluginFile: Path, pluginFileLock: FileLock?) =
      with(idePluginManager.createPlugin(pluginFile.toFile())) {
        when (this) {
          is PluginCreationSuccess -> findPluginClasses(plugin, warnings, pluginFileLock)
          is PluginCreationFail -> {
            pluginFileLock.closeLogged()
            PluginDetailsProvider.Result.InvalidPlugin(errorsAndWarnings)
          }
        }
      }

  private fun findPluginClasses(idePlugin: IdePlugin,
                                warnings: List<PluginProblem>,
                                pluginFileLock: FileLock?): PluginDetailsProvider.Result {
    val pluginClassesLocations = try {
      idePlugin.findPluginClasses()
    } catch (e: Exception) {
      LOG.info("Unable to read class files of $idePlugin", e)
      return PluginDetailsProvider.Result.InvalidPlugin(listOf(UnableToReadPluginClassFilesProblem()))
    }
    return PluginDetailsProvider.Result.Provided(
        PluginDetails(
            idePlugin,
            warnings,
            pluginClassesLocations,
            pluginFileLock
        )
    )
  }

  private fun IdePlugin.findPluginClasses() = IdePluginClassesFinder.findPluginClasses(this, additionalKeys = listOf(CompileServerExtensionKey))

}

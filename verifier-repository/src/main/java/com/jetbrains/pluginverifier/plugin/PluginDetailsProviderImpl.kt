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
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo
import java.nio.file.Path
import java.time.Duration

/**
 * Main implementation of the [PluginDetailsProvider] that
 * uses the [extractDirectory] for extracting `.zip`-ped plugins.
 */
class PluginDetailsProviderImpl(private val extractDirectory: Path) : PluginDetailsProvider {
  private val idePluginManager = IdePluginManager.createManager(extractDirectory.toFile())

  @Throws(IllegalArgumentException::class)
  override fun providePluginDetails(pluginFile: Path) = createPluginDetails(pluginFile, null, null)

  override fun providePluginDetails(
      pluginInfo: PluginInfo,
      pluginFileLock: FileLock
  ) = pluginFileLock.closeOnException {
    createPluginDetails(pluginFileLock.file, pluginFileLock, pluginInfo)
  }

  override fun providePluginDetails(
      pluginInfo: PluginInfo,
      idePlugin: IdePlugin
  ) = readPluginClasses(pluginInfo, idePlugin, emptyList(), null, Duration.ZERO, SpaceAmount.ZERO_SPACE)

  private fun createPluginDetails(
      pluginFile: Path,
      pluginFileLock: FileLock?,
      pluginInfo: PluginInfo?
  ) = with(idePluginManager.createPlugin(pluginFile.toFile())) {
    val fetchDuration = pluginFileLock?.fetchDuration ?: Duration.ZERO
    val pluginSize = pluginFileLock?.fileSize ?: SpaceAmount.ZERO_SPACE

    when (this) {
      is PluginCreationSuccess -> {
        readPluginClasses(
            pluginInfo ?: LocalPluginInfo(plugin),
            plugin,
            warnings,
            pluginFileLock,
            fetchDuration,
            pluginSize
        )
      }

      is PluginCreationFail -> {
        if (pluginInfo == null) {
          throw IllegalArgumentException("Invalid plugin from file $pluginFile: ${errorsAndWarnings.joinToString()}")
        }
        pluginFileLock.closeLogged()
        PluginDetailsProvider.Result.InvalidPlugin(pluginInfo, errorsAndWarnings, fetchDuration, pluginSize)
      }
    }
  }

  private fun readPluginClasses(
      pluginInfo: PluginInfo,
      idePlugin: IdePlugin,
      warnings: List<PluginProblem>,
      pluginFileLock: FileLock?,
      fetchDuration: Duration,
      pluginSize: SpaceAmount
  ): PluginDetailsProvider.Result {

    val pluginClassesLocations = try {
      IdePluginClassesFinder.findPluginClasses(idePlugin, additionalKeys = listOf(CompileServerExtensionKey))
    } catch (ie: InterruptedException) {
      throw ie
    } catch (e: Exception) {
      return PluginDetailsProvider.Result.Failed("Unable to read class files of $idePlugin", e)
    }

    return PluginDetailsProvider.Result.Provided(
        PluginDetails(
            pluginInfo,
            idePlugin,
            warnings,
            pluginClassesLocations,
            pluginFileLock
        ),
        fetchDuration,
        pluginSize
    )
  }

}

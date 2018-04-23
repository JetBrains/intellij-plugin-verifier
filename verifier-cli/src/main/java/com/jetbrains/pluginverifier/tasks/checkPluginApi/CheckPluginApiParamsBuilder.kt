package com.jetbrains.pluginverifier.tasks.checkPluginApi

import com.jetbrains.pluginverifier.dependencies.resolution.LastVersionSelector
import com.jetbrains.pluginverifier.dependencies.resolution.PluginVersionSelector
import com.jetbrains.pluginverifier.misc.closeOnException
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.PluginsSet
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tasks.TaskParametersBuilder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class CheckPluginApiParamsBuilder(private val pluginRepository: PluginRepository,
                                  private val pluginDetailsCache: PluginDetailsCache) : TaskParametersBuilder {
  companion object {
    private const val USAGE = """Expected arguments: <base plugin version> <new plugin version> <plugins to check>.
Example: java -jar verifier.jar check-plugin-api Kotlin-old.zip Kotlin-new.zip kotlin-depends.txt"""
  }

  override fun build(opts: CmdOpts, freeArgs: List<String>): CheckPluginApiParams {
    if (freeArgs.size != 3) {
      throw IllegalArgumentException(USAGE)
    }

    val basePluginFile = Paths.get(freeArgs[0])
    if (!basePluginFile.exists()) {
      throw IllegalArgumentException("Base plugin file $basePluginFile doesn't exist")
    }

    val newPluginFile = Paths.get(freeArgs[1])
    if (!newPluginFile.exists()) {
      throw IllegalArgumentException("New plugin file $newPluginFile doesn't exist")
    }

    val pluginsToCheckFile = File(freeArgs[2])
    if (!pluginsToCheckFile.exists()) {
      throw IllegalArgumentException("File with list of plugins' IDs to check $pluginsToCheckFile doesn't exist")
    }

    val pluginsSet = parsePluginsToCheck(pluginsToCheckFile)

    val jdkPath = OptionsParser.getJdkPath(opts)
    val problemsFilters = OptionsParser.getProblemsFilters(opts)

    val basePluginDetails = providePluginDetails(basePluginFile)
    basePluginDetails.closeOnException {
      val newPluginDetails = providePluginDetails(newPluginFile)
      newPluginDetails.closeOnException {
        return CheckPluginApiParams(
            pluginsSet,
            basePluginDetails,
            newPluginDetails,
            jdkPath,
            problemsFilters
        )
      }
    }
  }

  private fun providePluginDetails(pluginFile: Path) =
      with(pluginDetailsCache.pluginDetailsProvider.providePluginDetails(pluginFile)) {
        when (this) {
          is PluginDetailsProvider.Result.Provided -> pluginDetails
          is PluginDetailsProvider.Result.InvalidPlugin ->
            throw IllegalArgumentException("Plugin $pluginFile is invalid: \n" + pluginErrors.joinToString(separator = "\n") { it.message })
        }
      }

  private fun parsePluginsToCheck(pluginsToCheckFile: File): PluginsSet {
    val pluginsSet = PluginsSet()
    val lastVersionSelector = LastVersionSelector()
    pluginsToCheckFile.readLines().mapNotNull { pluginId ->
      with(lastVersionSelector.selectPluginVersion(pluginId, pluginRepository)) {
        when (this) {
          is PluginVersionSelector.Result.Selected -> pluginInfo
          else -> null
        }
      }
    }.forEach { pluginsSet.schedulePlugin(it) }
    return pluginsSet
  }


}
package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider.Result
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable
import java.nio.file.Path

/**
 * [Provides] [providePluginDetails] the [PluginDetails] of the plugins.
 * The possible results are represented as instances of [Result].
 */
interface PluginDetailsProvider {

  /**
   * Creates the [PluginDetails] for [plugin] [pluginInfo] which
   * files are locked with the [pluginFileLock].
   */
  fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock): Result

  /**
   * Creates [PluginDetails] for a plugin from [pluginFile].
   */
  fun providePluginDetails(pluginFile: Path): Result

  /**
   * Represents possible results of [providing] [providePluginDetails] the [PluginDetails].
   */
  sealed class Result : Closeable {

    data class Provided(val pluginDetails: PluginDetails) : Result() {
      override fun close() = pluginDetails.close()
    }

    data class InvalidPlugin(val pluginErrors: List<PluginProblem>) : Result() {
      override fun close() = Unit
    }

  }

}
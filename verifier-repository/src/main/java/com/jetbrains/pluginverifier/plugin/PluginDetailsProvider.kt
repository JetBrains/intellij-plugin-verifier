package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.pluginverifier.plugin.PluginDetailsProvider.Result
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable

/**
 * [Provides] [providePluginDetails] the [PluginDetails] of the plugins.
 * The possible results are represented as instances of [Result].
 */
interface PluginDetailsProvider {

  /**
   * Creates the [PluginDetails] for the [plugin] [pluginInfo] which
   * files are locked with the [pluginFileLock].
   */
  fun providePluginDetails(pluginInfo: PluginInfo, pluginFileLock: FileLock): Result

  /**
   * Represents possible results of [providing] [providePluginDetails] the [PluginDetails].
   */
  sealed class Result : Closeable {

    class Provided(val pluginDetails: PluginDetails) : Result() {
      override fun close() = pluginDetails.close()
    }

    class InvalidPlugin(val pluginErrors: List<PluginProblem>) : Result() {
      override fun close() = Unit
    }

  }

}
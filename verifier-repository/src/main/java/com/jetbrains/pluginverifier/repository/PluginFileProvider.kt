package com.jetbrains.pluginverifier.repository

import com.jetbrains.pluginverifier.repository.files.FileLock

/**
 * Provides files of [PluginInfo] locked with [FileLock]s.
 */
interface PluginFileProvider {

  /**
   * Finds a file of the [pluginInfo] and wraps
   * it with [FileLock] if necessary.
   */
  fun getPluginFile(pluginInfo: PluginInfo): Result

  /**
   * Represents possible outcomes of [fetching] [getPluginFile] the plugin's file.
   */
  sealed class Result {

    data class Found(val pluginFileLock: FileLock) : Result()

    data class NotFound(val reason: String) : Result()

    data class Failed(val reason: String, val error: Exception) : Result()
  }

}
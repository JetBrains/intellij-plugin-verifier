package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.FileLock

/**
 * @author Sergey Patrikeev
 */
interface PluginFileFinder {
  fun findPluginFile(): Result

  sealed class Result {
    data class Found(val pluginFileLock: FileLock) : Result()
    data class FailedToDownload(val reason: String) : Result()
    data class NotFound(val reason: String) : Result()
  }
}
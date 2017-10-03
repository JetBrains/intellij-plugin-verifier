package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.DownloadPluginResult
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo

class RepositoryPluginFileFinder(private val pluginRepository: PluginRepository,
                                 private val updateInfo: UpdateInfo) : PluginFileFinder {

  override fun findPluginFile(): PluginFileFinder.Result {
    val downloadPluginResult = pluginRepository.downloadPluginFile(updateInfo)
    return when (downloadPluginResult) {
      is DownloadPluginResult.Found -> PluginFileFinder.Result.Found(downloadPluginResult.fileLock)
      is DownloadPluginResult.NotFound -> PluginFileFinder.Result.NotFound(downloadPluginResult.reason)
      is DownloadPluginResult.FailedToDownload -> PluginFileFinder.Result.FailedToDownload(downloadPluginResult.reason)
    }
  }
}
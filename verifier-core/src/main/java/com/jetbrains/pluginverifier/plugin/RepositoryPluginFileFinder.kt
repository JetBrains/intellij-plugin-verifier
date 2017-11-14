package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult

class RepositoryPluginFileFinder(private val pluginRepository: PluginRepository,
                                 private val updateInfo: UpdateInfo) : PluginFileFinder {

  override fun findPluginFile(): PluginFileFinder.Result {
    val downloadPluginResult = pluginRepository.downloadPluginFile(updateInfo)
    return with(downloadPluginResult) {
      when (this) {
        is FileRepositoryResult.Found -> PluginFileFinder.Result.Found(lockedFile)
        is FileRepositoryResult.NotFound -> PluginFileFinder.Result.NotFound(reason)
        is FileRepositoryResult.Failed -> PluginFileFinder.Result.FailedToDownload(reason)
      }
    }
  }
}
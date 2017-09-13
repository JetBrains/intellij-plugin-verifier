package com.jetbrains.pluginverifier.repository

sealed class DownloadPluginResult {
  data class Found(val updateInfo: UpdateInfo, val fileLock: FileLock) : DownloadPluginResult()

  data class NotFound(val updateInfo: UpdateInfo, val reason: String) : DownloadPluginResult()

  data class FailedToDownload(val updateInfo: UpdateInfo, val reason: String) : DownloadPluginResult()
}
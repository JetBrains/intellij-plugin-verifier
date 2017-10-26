package com.jetbrains.pluginverifier.repository

sealed class DownloadPluginResult {
  data class Found(val fileLock: FileLock) : DownloadPluginResult()

  data class NotFound(val reason: String) : DownloadPluginResult()

  data class FailedToDownload(val reason: String) : DownloadPluginResult()
}
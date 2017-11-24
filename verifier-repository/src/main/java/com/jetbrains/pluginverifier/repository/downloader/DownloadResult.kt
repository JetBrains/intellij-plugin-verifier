package com.jetbrains.pluginverifier.repository.downloader

import java.nio.file.Path

sealed class DownloadResult {
  data class Downloaded(val downloadedFileOrDirectory: Path, val extension: String, val isDirectory: Boolean) : DownloadResult()

  data class NotFound(val reason: String) : DownloadResult()

  data class FailedToDownload(val reason: String, val error: Exception) : DownloadResult()
}
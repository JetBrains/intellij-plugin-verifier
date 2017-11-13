package com.jetbrains.pluginverifier.repository.downloader

import java.io.File

sealed class DownloadResult {
  data class Downloaded(val file: File) : DownloadResult()

  data class NotFound(val reason: String) : DownloadResult()

  data class FailedToDownload(val reason: String, val error: Exception) : DownloadResult()
}

interface Downloader<in K> {
  fun download(coordinate: K): DownloadResult
}
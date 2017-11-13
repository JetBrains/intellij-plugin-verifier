package com.jetbrains.pluginverifier.repository.cleanup

import com.jetbrains.pluginverifier.repository.DownloadManager

interface FileSweeper {
  fun sweep(downloadManager: DownloadManager)
}
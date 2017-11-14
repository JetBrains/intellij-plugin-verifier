package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import java.io.File

class MockDownloader : Downloader<Int> {
  override fun download(destinationDirectory: File, key: Int): DownloadResult {
    val file = destinationDirectory.resolve(key.toString())
    file.writeText(key.toString())
    return DownloadResult.Downloaded(file, "")
  }
}
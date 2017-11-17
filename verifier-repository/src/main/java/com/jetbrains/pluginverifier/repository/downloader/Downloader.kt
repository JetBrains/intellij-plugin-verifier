package com.jetbrains.pluginverifier.repository.downloader

import java.io.File

interface Downloader<in K> {
  fun download(key: K, tempDirectory: File): DownloadResult
}
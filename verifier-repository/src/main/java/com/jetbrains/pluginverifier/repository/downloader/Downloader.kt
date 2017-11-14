package com.jetbrains.pluginverifier.repository.downloader

import java.io.File

interface Downloader<in K> {
  fun download(destinationDirectory: File, key: K): DownloadResult
}
package com.jetbrains.pluginverifier.repository.downloader

import java.nio.file.Path

interface Downloader<in K> {
  fun download(key: K, tempDirectory: Path): DownloadResult
}
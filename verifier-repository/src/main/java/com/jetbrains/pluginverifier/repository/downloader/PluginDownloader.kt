package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.repository.UpdateId
import java.net.URL
import java.nio.file.Path

class PluginDownloader(private val pluginRepositoryUrl: String) : Downloader<UpdateId> {

  private val urlDownloader = UrlDownloader<UpdateId> {
    URL(pluginRepositoryUrl.trimEnd('/') + "/plugin/download/?noStatistic=true&updateId=${it.id}")
  }

  override fun download(key: UpdateId, tempDirectory: Path) = with(urlDownloader.download(key, tempDirectory)) {
    when (this) {
      is DownloadResult.Downloaded -> this
      is DownloadResult.FailedToDownload -> DownloadResult.FailedToDownload("Plugin $key is not downloaded: " + error.message, error)
      is DownloadResult.NotFound -> DownloadResult.NotFound("Plugin $key is not found in the $pluginRepositoryUrl")
    }
  }
}
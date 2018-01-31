package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.repository.PluginInfo
import java.nio.file.Path

object PluginDownloader : Downloader<PluginInfo> {

  private val urlDownloader = UrlDownloader<PluginInfo> { it.downloadUrl }

  override fun download(key: PluginInfo, tempDirectory: Path) =
      with(urlDownloader.download(key, tempDirectory)) {
        when (this) {
          is DownloadResult.Downloaded -> this
          is DownloadResult.FailedToDownload -> DownloadResult.FailedToDownload("Plugin $key is not downloaded: " + error.message, error)
          is DownloadResult.NotFound -> DownloadResult.NotFound("Plugin $key is not found in the Plugin Repository: $reason")
        }
      }
}
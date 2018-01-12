package com.jetbrains.pluginverifier.repository.downloader

import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.nio.file.Path

/**
 * [ResourceProvider] that downloads a file or directory using the [downloadExecutor].
 */
class DownloadProvider<K>(private val downloadExecutor: DownloadExecutor<K>) : ResourceProvider<K, Path> {
  override fun provide(key: K): ProvideResult<Path> {
    val download = downloadExecutor.download(key)
    return with(download) {
      when (this) {
        is DownloadResult.Downloaded -> ProvideResult.Provided(downloadedFileOrDirectory)
        is DownloadResult.NotFound -> ProvideResult.NotFound(reason)
        is DownloadResult.FailedToDownload -> ProvideResult.Failed(reason, error)
      }
    }
  }
}
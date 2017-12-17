package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import com.jetbrains.pluginverifier.repository.downloader.DownloadExecutor
import com.jetbrains.pluginverifier.repository.downloader.DownloadResult
import com.jetbrains.pluginverifier.repository.downloader.SpaceWeight
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryImpl
import java.nio.file.Path
import java.time.Clock

/**
 * Resource provider that downloads a file or directory using the [downloadExecutor].
 */
private class DownloadProvider<K>(private val downloadExecutor: DownloadExecutor<K>) : ResourceProvider<K, Path> {
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

/**
 * The implementation of the file repository which
 * can be safely used in a concurrent environment where
 * multiple threads can add, use and remove the files.
 */
class FileRepositoryImpl<K>(sweepPolicy: SweepPolicy<K>,
                            clock: Clock,
                            downloadExecutor: DownloadExecutor<K>) : FileRepository<K> {

  private val resourceRepository = ResourceRepositoryImpl(
      sweepPolicy,
      clock,
      DownloadProvider(downloadExecutor),
      SpaceWeight(SpaceAmount.ZERO_SPACE),
      { SpaceWeight(it.fileSize) },
      { path -> path.deleteLogged() }
  )


  /**
   * Provides the file by [key]. The file is returned from the
   * file repository cache or is [downloaded] [DownloadProvider].
   *
   * This method is thread safe. In case several threads attempt to get the same file, only one
   * of them will download it while others will wait for the first to complete.
   */
  override fun get(key: K) = resourceRepository.get(key)

  override fun add(key: K, resource: Path) = resourceRepository.add(key, resource)

  override fun remove(key: K) = resourceRepository.remove(key)

  override fun has(key: K) = resourceRepository.has(key)

  override fun getAllExistingKeys() = resourceRepository.getAllExistingKeys()

  override fun <R> lockAndExecute(block: () -> R) = resourceRepository.lockAndExecute(block)

  override fun cleanup() = resourceRepository.cleanup()

}
package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.utils.nameWithoutExtension
import com.jetbrains.plugin.structure.base.utils.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.repository.Downloadable
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.DownloadProvider
import com.jetbrains.pluginverifier.repository.downloader.DownloadStatistics
import com.jetbrains.pluginverifier.repository.downloader.UrlDownloader
import com.jetbrains.pluginverifier.repository.files.*
import com.jetbrains.pluginverifier.repository.repositories.marketplace.MarketplaceRepository
import com.jetbrains.pluginverifier.repository.repositories.marketplace.UpdateInfo
import org.apache.commons.io.FileUtils
import java.net.URL
import java.nio.file.Path

/**
 * Storage of plugin files cached locally.
 *
 * Each plugin is identified by its [PluginInfo] and can be accessed in [getPluginFile].
 * A lock is registered for the plugin file to avoid use-remove conflicts
 * when one thread uses the file and another thread deletes it.
 */
class PluginFilesBank(
    private val fileRepository: FileRepository<PluginInfo>,
    private val urlProvider: (PluginInfo) -> URL?,
    private val downloadProvider: DownloadProvider<PluginInfo>
) : PluginFileProvider {

  companion object {
    fun create(
        repository: PluginRepository,
        pluginsDir: Path,
        diskSpaceSetting: DiskSpaceSetting
    ): PluginFilesBank {
      val sweepPolicy = LruFileSizeSweepPolicy<PluginInfo>(diskSpaceSetting)

      val urlProvider: (PluginInfo) -> URL? = { (it as? Downloadable)?.downloadUrl }
      val urlDownloader = UrlDownloader(urlProvider)

      val downloadProvider = DownloadProvider(pluginsDir, urlDownloader, PluginFileNameMapper)

      val fileRepository = FileRepositoryBuilder<PluginInfo>()
          .sweepPolicy(sweepPolicy)
          .resourceProvider(downloadProvider)
          .presentableName("downloaded-plugins")
          .addInitialFilesFrom(pluginsDir) { getPluginInfoByFile(repository, it) }
          .build()

      return PluginFilesBank(fileRepository, urlProvider, downloadProvider)
    }

    private fun getPluginInfoByFile(repository: PluginRepository, file: Path): PluginInfo? {
      val name = file.nameWithoutExtension
      val updateId = name.toIntOrNull()
      if (updateId != null && repository is MarketplaceRepository) {
        return repository.getPluginInfoById(updateId)
      }
      val pluginId = name.substringBefore("-")
      val version = name.substringAfter("-")
      return repository.getAllVersionsOfPlugin(pluginId).find { it.version == version }
    }

  }

  val downloadStatistics: DownloadStatistics
    get() = downloadProvider.downloadStatistics

  /**
   * Returns a plugin file for the [pluginInfo] that
   * can be cached locally or downloaded from the repository.
   *
   * The result is wrapped into the [FileRepositoryResult] to
   * indicate possible outcomes of the fetching.
   * In case a [FileRepositoryResult.Found] is returned,
   * a file lock is registered for the plugin file to protect it against
   * deletion or eviction while the file is used.
   */
  override fun getPluginFile(pluginInfo: PluginInfo): PluginFileProvider.Result {
    val downloadUrl = urlProvider(pluginInfo)
        ?: return PluginFileProvider.Result.NotFound("Cannot find plugin $pluginInfo")
    if (downloadUrl.protocol == "file") {
      val file = FileUtils.toFile(downloadUrl)
      return if (file.exists()) {
        PluginFileProvider.Result.Found(IdleFileLock(file.toPath()))
      } else {
        PluginFileProvider.Result.NotFound("Plugin file doesn't exist: $file")
      }
    }
    return with(fileRepository.getFile(pluginInfo)) {
      when (this) {
        is FileRepositoryResult.Found -> PluginFileProvider.Result.Found(lockedFile)
        is FileRepositoryResult.NotFound -> PluginFileProvider.Result.NotFound(reason)
        is FileRepositoryResult.Failed -> PluginFileProvider.Result.Failed(reason, error)
      }
    }
  }

  /**
   * Returns a set of plugins available locally at the moment.
   */
  fun getAvailablePluginFiles() = fileRepository.getAvailableFiles()
}

object PluginFileNameMapper : FileNameMapper<PluginInfo> {
  override fun getFileNameWithoutExtension(key: PluginInfo) = when (key) {
    is UpdateInfo -> key.updateId.toString()
    else -> (key.pluginId + "-" + key.version).replaceInvalidFileNameCharacters()
  }
}
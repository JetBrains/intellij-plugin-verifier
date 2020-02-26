package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.base.utils.listFiles
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
import com.jetbrains.pluginverifier.repository.files.FileRepository
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import com.jetbrains.pluginverifier.repository.files.IdleFileLock
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
      pluginRepository: PluginRepository,
      pluginsDir: Path,
      diskSpaceSetting: DiskSpaceSetting
    ): PluginFilesBank {
      val sweepPolicy = LruFileSizeSweepPolicy<PluginInfo>(diskSpaceSetting)

      val urlProvider: (PluginInfo) -> URL? = { (it as? Downloadable)?.downloadUrl }
      val urlDownloader = UrlDownloader(urlProvider)

      val downloadProvider = DownloadProvider(pluginsDir, urlDownloader) { key ->
        when (key) {
          is UpdateInfo -> getFileNameForMarketplacePlugin(key)
          else -> (key.pluginId + "-" + key.version).replaceInvalidFileNameCharacters()
        }
      }

      val fileRepository = FileRepository(
        downloadProvider,
        sweepPolicy,
        "downloaded-plugins"
      )

      if (pluginRepository is MarketplaceRepository) {
        addAlreadyDownloadedPlugins(pluginsDir, pluginRepository, fileRepository)
      }

      return PluginFilesBank(fileRepository, urlProvider, downloadProvider)
    }

    private fun getFileNameForMarketplacePlugin(pluginInfo: UpdateInfo): String =
      "${pluginInfo.pluginIntId}_${pluginInfo.updateId}"

    private fun getPluginIdAndUpdateIdByPath(path: Path): Pair<Int, Int>? {
      val fileName = path.nameWithoutExtension
      val pluginId = fileName.substringBefore("_", "").toIntOrNull() ?: return null
      val updateId = fileName.substringAfter("_", "").toIntOrNull() ?: return null
      return pluginId to updateId
    }

    private fun addAlreadyDownloadedPlugins(
      pluginsDir: Path,
      pluginRepository: MarketplaceRepository,
      fileRepository: FileRepository<PluginInfo>
    ) {
      val pathToPluginIdAndUpdateId = hashMapOf<Path, Pair<Int, Int>>()
      for (path in pluginsDir.listFiles()) {
        val pluginIdAndUpdateId = getPluginIdAndUpdateIdByPath(path)
        if (pluginIdAndUpdateId != null) {
          pathToPluginIdAndUpdateId[path] = pluginIdAndUpdateId
        } else {
          path.deleteLogged()
        }
      }

      //Batch request many update infos. Much faster than N > 1000 individual requests.
      val updateIdToUpdateInfo = pluginRepository.getPluginInfosForManyIds(
        pathToPluginIdAndUpdateId.map { it.value }
      )

      for ((path, pluginIdAndUpdateId) in pathToPluginIdAndUpdateId) {
        val updateInfo = updateIdToUpdateInfo[pluginIdAndUpdateId.second]
        if (updateInfo != null) {
          fileRepository.add(updateInfo, path)
        } else {
          path.deleteLogged()
        }
      }
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
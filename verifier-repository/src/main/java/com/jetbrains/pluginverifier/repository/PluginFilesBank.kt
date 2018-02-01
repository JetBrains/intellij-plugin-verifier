package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.DownloadProvider
import com.jetbrains.pluginverifier.repository.downloader.UrlDownloader
import com.jetbrains.pluginverifier.repository.files.*
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
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
class PluginFilesBank(private val fileRepository: FileRepository<PluginInfo>) {

  companion object {
    fun create(repository: PluginRepository,
               pluginsDir: Path,
               diskSpaceSetting: DiskSpaceSetting): PluginFilesBank {
      val sweepPolicy = LruFileSizeSweepPolicy<PluginInfo>(diskSpaceSetting)

      val urlDownloader = UrlDownloader<PluginInfo> { it.downloadUrl }

      val downloadProvider = DownloadProvider(pluginsDir, urlDownloader, PluginFileNameMapper)
      val pluginFileResourceProvider = PluginFileResourceProvider(downloadProvider)

      val fileRepository = FileRepositoryBuilder<PluginInfo>()
          .sweepPolicy(sweepPolicy)
          .resourceProvider(pluginFileResourceProvider)
          .presentableName("downloaded-plugins")
          .addInitialFilesFrom(pluginsDir) { getPluginInfoByFile(repository, it) }
          .build()

      return PluginFilesBank(fileRepository)
    }

    private fun getPluginInfoByFile(repository: PluginRepository, file: Path): PluginInfo? {
      val name = file.nameWithoutExtension
      val updateId = Ints.tryParse(name)
      if (updateId != null && repository is PublicPluginRepository) {
        return repository.getPluginInfoById(updateId)
      }
      val pluginId = name.substringBefore("-")
      val version = name.substringAfter("-")
      return repository.getAllPlugins().find { it.pluginId == pluginId && it.version == version }
    }

  }

  /**
   * Returns a plugin file for the [pluginInfo] that
   * can be cached locally or downloaded from the [repository].
   *
   * The result is wrapped into the [FileRepositoryResult] to
   * indicate possible outcomes of the fetching.
   * In case a [FileRepositoryResult.Found] is returned,
   * a [lock] [com.jetbrains.pluginverifier.repository.files.FileLock]
   * is registered for the plugin file to protect it against
   * deletion or eviction while the file is used.
   */
  fun getPluginFile(pluginInfo: PluginInfo): Result {
    if (pluginInfo.downloadUrl == null) {
      return Result.InMemoryPlugin(pluginInfo.idePlugin!!)
    }
    return with(fileRepository.getFile(pluginInfo)) {
      when (this) {
        is FileRepositoryResult.Found -> Result.Found(lockedFile)
        is FileRepositoryResult.NotFound -> Result.NotFound(reason)
        is FileRepositoryResult.Failed -> Result.Failed(reason, error)
      }
    }
  }

  /**
   * Represents possible outcomes of [fetching] [getPluginFile] the plugin's file.
   */
  sealed class Result {

    data class Found(val pluginFileLock: FileLock) : Result()

    data class NotFound(val reason: String) : Result()

    data class Failed(val reason: String, val error: Exception) : Result()

    data class InMemoryPlugin(val idePlugin: IdePlugin) : Result()
  }

}

object PluginFileNameMapper : FileNameMapper<PluginInfo> {
  override fun getFileNameWithoutExtension(key: PluginInfo) = when (key) {
    is UpdateInfo -> key.updateId.toString()
    else -> (key.pluginId + "-" + key.version).replaceInvalidFileNameCharacters()
  }
}

class PluginFileResourceProvider(private val downloadProvider: DownloadProvider<PluginInfo>) : ResourceProvider<PluginInfo, Path> {

  override fun provide(key: PluginInfo): ProvideResult<Path> {
    val downloadUrl = key.downloadUrl ?: return ProvideResult.NotFound("Plugin file of $key is not found")
    if (downloadUrl.protocol == "file") {
      /**
       * Though the [DownloadProvider] is capable to provide local files by
       * URLs with `file://` scheme, it may require files copying to ensure
       * that the source file will not be corrupted.
       * In the current case, it is safe to return the plugin's file directly.
       */
      return providedLocalFile(downloadUrl)
    }
    return downloadProvider.provide(key)
  }

  private fun providedLocalFile(downloadUrl: URL): ProvideResult<Path> {
    val file = FileUtils.toFile(downloadUrl)
    return if (file.exists()) {
      ProvideResult.Provided(file.toPath())
    } else {
      ProvideResult.NotFound("Plugin file doesn't exist: $file")
    }
  }

}
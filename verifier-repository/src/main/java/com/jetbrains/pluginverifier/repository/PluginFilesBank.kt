package com.jetbrains.pluginverifier.repository

import com.google.common.primitives.Ints
import com.jetbrains.pluginverifier.misc.nameWithoutExtension
import com.jetbrains.pluginverifier.misc.replaceInvalidFileNameCharacters
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.PluginDownloader
import com.jetbrains.pluginverifier.repository.files.FileNameMapper
import com.jetbrains.pluginverifier.repository.files.FileRepository
import com.jetbrains.pluginverifier.repository.files.FileRepositoryBuilder
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import java.nio.file.Path

/**
 * Storage of plugin files cached locally.
 *
 * Each plugin is identified by its [PluginInfo] and can be accessed in [getPluginFile].
 * A lock is registered for the plugin file to avoid use-remove conflicts
 * when one thread uses the file and another thread deletes it.
 */
class PluginFilesBank(private val repository: PluginRepository,
                      private val fileRepository: FileRepository<PluginInfo>) {

  companion object {
    fun create(repository: PluginRepository,
               downloadDir: Path,
               diskSpaceSetting: DiskSpaceSetting): PluginFilesBank {
      val sweepPolicy = LruFileSizeSweepPolicy<PluginInfo>(diskSpaceSetting)

      val fileRepository = FileRepositoryBuilder().createFromExistingFiles(
          downloadDir,
          PluginDownloader,
          PluginFileNameMapper,
          sweepPolicy,
          keyProvider = { getPluginInfoByFile(repository, it) },
          presentableName = "downloaded-plugins"
      )

      return PluginFilesBank(repository, fileRepository)
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
  fun getPluginFile(pluginInfo: PluginInfo): FileRepositoryResult = fileRepository.getFile(pluginInfo)

}

object PluginFileNameMapper : FileNameMapper<PluginInfo> {
  override fun getFileNameWithoutExtension(key: PluginInfo) = when (key) {
    is UpdateInfo -> key.updateId.toString()
    else -> (key.pluginId + "-" + key.version).replaceInvalidFileNameCharacters()
  }
}



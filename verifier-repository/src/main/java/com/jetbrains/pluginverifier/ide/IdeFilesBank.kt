package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.DownloadProvider
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.FileRepositoryBuilder
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import java.nio.file.Path

/**
 * Storage of IDE builds kept locally.
 *
 * Each IDE is identified by its [IdeVersion] and can be locked for the use time
 * to avoid use-remove conflicts when one thread uses the IDE build and another
 * thread deletes it.
 */
class IdeFilesBank(private val bankDirectory: Path,
                   ideRepository: IdeRepository,
                   diskSpaceSetting: DiskSpaceSetting) {

  private val ideFilesRepository = FileRepositoryBuilder<IdeVersion>()
      .sweepPolicy(LruFileSizeSweepPolicy(diskSpaceSetting))
      .resourceProvider(DownloadProvider(bankDirectory, IdeDownloader(ideRepository), IdeFileNameMapper()))
      .presentableName("IDEs bank at $bankDirectory")
      .addInitialFilesFrom(bankDirectory) { IdeFileNameMapper.getIdeVersionByFile(it) }
      .build()

  fun <R> lockAndAccess(block: () -> R) = ideFilesRepository.lockAndExecute(block)

  fun getAvailableIdeVersions() = ideFilesRepository.getAllExistingKeys()

  fun getAvailableIdeFiles() = ideFilesRepository.getAvailableFiles()

  fun isAvailable(ideVersion: IdeVersion) = ideFilesRepository.has(ideVersion)

  fun deleteIde(ideVersion: IdeVersion) = ideFilesRepository.remove(ideVersion)

  fun getIdeFile(ideVersion: IdeVersion): Result =
      with(ideFilesRepository.getFile(ideVersion)) {
        when (this) {
          is FileRepositoryResult.Found -> Result.Found(lockedFile)
          is FileRepositoryResult.NotFound -> Result.NotFound(reason)
          is FileRepositoryResult.Failed -> Result.Failed(reason, error)
        }
      }

  sealed class Result {
    data class Found(val ideFileLock: FileLock) : Result()

    data class NotFound(val reason: String) : Result()

    data class Failed(val reason: String, val exception: Exception) : Result()
  }

  override fun toString() = "IDEs at $bankDirectory"

}
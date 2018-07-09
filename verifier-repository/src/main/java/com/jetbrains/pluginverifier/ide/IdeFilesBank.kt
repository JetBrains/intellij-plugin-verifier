package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.ide.IdeFilesBank.Result.Found
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.DownloadProvider
import com.jetbrains.pluginverifier.repository.files.AvailableFile
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.FileRepositoryBuilder
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import java.nio.file.Path

/**
 * Storage of IDE builds, which are kept under the [bankDirectory].
 *
 * Each IDE is identified by its [IdeVersion] and can be locked for the use time
 * to avoid use-remove conflicts when one thread uses the IDE build and another
 * thread deletes it.
 */
class IdeFilesBank(
    private val bankDirectory: Path,
    ideRepository: IdeRepository,
    diskSpaceSetting: DiskSpaceSetting
) {

  private val ideFilesRepository = FileRepositoryBuilder<IdeVersion>()
      .sweepPolicy(LruFileSizeSweepPolicy(diskSpaceSetting))
      .resourceProvider(DownloadProvider(bankDirectory, IdeDownloader(ideRepository), IdeFileNameMapper()))
      .presentableName("IDEs bank at $bankDirectory")
      .addInitialFilesFrom(bankDirectory) { IdeFileNameMapper.getIdeVersionByFile(it) }
      .build()

  fun getAvailableIdeVersions(): Set<IdeVersion> =
      ideFilesRepository.getAllExistingKeys()

  fun getAvailableIdeFiles(): List<AvailableFile<IdeVersion>> =
      ideFilesRepository.getAvailableFiles()

  fun isAvailable(ideVersion: IdeVersion): Boolean =
      ideFilesRepository.has(ideVersion)

  fun deleteIde(ideVersion: IdeVersion): Boolean =
      ideFilesRepository.remove(ideVersion)

  @Throws(InterruptedException::class)
  fun getIdeFile(ideVersion: IdeVersion): Result =
      with(ideFilesRepository.getFile(ideVersion)) {
        when (this) {
          is FileRepositoryResult.Found -> Result.Found(lockedFile)
          is FileRepositoryResult.NotFound -> Result.NotFound(reason)
          is FileRepositoryResult.Failed -> Result.Failed(reason, error)
        }
      }

  /**
   * Result of [getting] [getIdeFile] IDE file.
   *
   * [Found] result contains a [FileLock] that must
   * be closed after usage.
   */
  sealed class Result {
    /**
     * IDE build is found.
     *
     * [ideFileLock] is registered for this IDE
     * to protect it from deletions while it is used.
     * It must be [closed] [FileLock.close] after usage.
     */
    data class Found(val ideFileLock: FileLock) : Result()

    /**
     * IDE is not found due to [reason].
     */
    data class NotFound(val reason: String) : Result()

    /**
     * IDE is failed to be found because of [reason]
     * and an aroused [exception].
     */
    data class Failed(val reason: String, val exception: Exception) : Result()
  }

  override fun toString() = "IDEs at $bankDirectory"

}
package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.FileRepositoryBuilder
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
                   diskSpaceSetting: DiskSpaceSetting,
                   downloadProgress: (Double) -> Unit) {

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(IdeFilesBank::class.java)
  }

  private val ideFilesRepository = FileRepositoryBuilder().createFromExistingFiles(
      bankDirectory,
      IdeDownloader(ideRepository, downloadProgress),
      IdeFileNameMapper(),
      LruFileSizeSweepPolicy(diskSpaceSetting),
      keyProvider = { IdeFileNameMapper.getIdeVersionByFile(it) },
      presentableName = "IDEs"
  )

  fun <R> lockAndAccess(block: () -> R) = ideFilesRepository.lockAndExecute(block)

  fun getAvailableIdeVersions() = ideFilesRepository.getAllExistingKeys()

  fun isAvailable(ideVersion: IdeVersion) = ideFilesRepository.has(ideVersion)

  fun deleteIde(ideVersion: IdeVersion) = ideFilesRepository.remove(ideVersion)

  fun getIdeFileLock(ideVersion: IdeVersion): FileLock? = with(ideFilesRepository.getFile(ideVersion)) {
    when (this) {
      is FileRepositoryResult.Found -> lockedFile
      is FileRepositoryResult.NotFound -> {
        LOG.info("IDE $ideVersion is not found: $reason")
        null
      }
      is FileRepositoryResult.Failed -> {
        LOG.info("Unable to download IDE $ideVersion: $reason", error)
        null
      }
    }
  }

  override fun toString() = "IDEs at $bankDirectory"

}
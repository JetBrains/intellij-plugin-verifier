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

//todo: provide a cache of IdeDescriptors
class IdeFilesBank(val ideRepository: IdeRepository,
                   bankDirectory: Path,
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
      keyProvider = { IdeFileNameMapper.getIdeVersionByFile(it) }
  )

  fun <R> lockAndAccess(block: () -> R) =
      ideFilesRepository.lockAndAccess(block)

  fun getAvailableIdeVersions() =
      ideFilesRepository.getAllExistingKeys()

  fun has(key: IdeVersion) =
      ideFilesRepository.has(key)

  fun deleteIde(key: IdeVersion) =
      ideFilesRepository.remove(key)

  fun getIdeLock(key: IdeVersion): FileLock? = with(ideFilesRepository.get(key)) {
    when (this) {
      is FileRepositoryResult.Found -> lockedFile
      is FileRepositoryResult.NotFound -> {
        LOG.info("IDE $key is not found: $reason")
        null
      }
      is FileRepositoryResult.Failed -> {
        LOG.info("Unable to download IDE $key: $reason", error)
        null
      }
    }
  }
}
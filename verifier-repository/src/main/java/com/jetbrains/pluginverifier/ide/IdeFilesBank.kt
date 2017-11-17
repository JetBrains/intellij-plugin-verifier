package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.repository.cleanup.DiskSpaceSetting
import com.jetbrains.pluginverifier.repository.cleanup.LruFileSizeSweepPolicy
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.files.FileRepositoryImpl
import com.jetbrains.pluginverifier.repository.files.FileRepositoryResult
import java.io.File

//todo: provide a cache of IdeDescriptors
class IdeFilesBank(val ideRepository: IdeRepository,
                   bankDirectory: File,
                   diskSpaceSetting: DiskSpaceSetting,
                   downloadProgress: (Double) -> Unit) {

  private val ideFilesRepository = FileRepositoryImpl(
      bankDirectory,
      IdeDownloader(ideRepository, downloadProgress),
      IdeFileKeyMapper(),
      LruFileSizeSweepPolicy(diskSpaceSetting)
  )

  fun <R> lockAndAccess(block: () -> R) =
      ideFilesRepository.lockAndAccess(block)

  fun getAvailableIdeVersions() =
      ideFilesRepository.getAllExistingKeys()

  fun has(key: IdeVersion) =
      ideFilesRepository.has(key)

  fun deleteIde(key: IdeVersion) =
      ideFilesRepository.remove(key)

  fun getIdeLock(key: IdeVersion): FileLock? =
      (ideFilesRepository.get(key) as? FileRepositoryResult.Found)?.lockedFile
}
package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.DownloadExecutor
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import java.nio.file.Path
import java.time.Clock

class FileRepositoryBuilder {
  fun <K> createFromExistingFiles(repositoryDir: Path,
                                  downloader: Downloader<K>,
                                  fileNameMapper: FileNameMapper<K>,
                                  sweepPolicy: SweepPolicy<K>,
                                  clock: Clock = Clock.systemUTC(),
                                  keyProvider: (Path) -> K? = { null }): FileRepository<K> {
    val downloadExecutor = DownloadExecutor(repositoryDir, downloader, fileNameMapper)
    val fileRepository = FileRepositoryImpl(sweepPolicy, clock, downloadExecutor)
    readAvailableFiles(fileRepository, keyProvider, downloadExecutor)
    fileRepository.cleanup()
    return fileRepository
  }

  private fun <K> readAvailableFiles(fileRepository: FileRepositoryImpl<K>,
                                     keyProvider: (Path) -> K?,
                                     downloadExecutor: DownloadExecutor<K>) {
    val existingFiles = downloadExecutor.getAvailableFiles()
    for (file in existingFiles) {
      val key = keyProvider(file)
      if (key != null) {
        fileRepository.add(key, file)
      } else {
        file.deleteLogged()
      }
    }
  }
}


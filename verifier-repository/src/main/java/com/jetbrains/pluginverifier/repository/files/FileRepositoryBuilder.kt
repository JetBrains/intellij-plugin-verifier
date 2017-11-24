package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.downloader.DownloadExecutor
import com.jetbrains.pluginverifier.repository.downloader.Downloader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock

/**
 * @author Sergey Patrikeev
 */
fun <K> createFromExistingFiles(repositoryDir: Path,
                                downloader: Downloader<K>,
                                fileNameMapper: FileNameMapper<K>,
                                sweepPolicy: SweepPolicy<K>,
                                clock: Clock = Clock.systemUTC(),
                                keyProvider: (Path) -> K? = { null }): FileRepository<K> {
  val downloadExecutor = DownloadExecutor(repositoryDir, downloader, fileNameMapper)
  val fileRepository = FileRepositoryImpl(sweepPolicy, clock, downloadExecutor)
  addInitiallyAvailableFiles(fileRepository, repositoryDir, keyProvider)
  fileRepository.sweep()
  return fileRepository
}

private fun <K> addInitiallyAvailableFiles(fileRepository: FileRepository<K>,
                                           repositoryDir: Path,
                                           keyProvider: (Path) -> K?) {
  val existingFiles = Files.list(repositoryDir) ?: throw IOException("Unable to read directory content: $repositoryDir")
  for (file in existingFiles) {
    val key = keyProvider(file)
    if (key != null) {
      fileRepository.add(key, file)
    }
  }
}


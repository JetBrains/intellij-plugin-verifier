package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.misc.deleteLogged
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.listFiles
import com.jetbrains.pluginverifier.repository.cleanup.IdleSweepPolicy
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.provider.EmptyResourceProvider
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import java.nio.file.Path
import java.time.Clock

class FileRepositoryBuilder<K> {

  private var clock: Clock = Clock.systemUTC()

  private var presentableName: String = "File repository"

  private var sweepPolicy: SweepPolicy<K> = IdleSweepPolicy()

  private var resourceProvider: ResourceProvider<K, Path> = EmptyResourceProvider()

  private var initDirectory: Path? = null

  private var keyProvider: ((Path) -> K?)? = null

  fun build(): FileRepository<K> {
    val fileRepository = FileRepository(
        sweepPolicy,
        resourceProvider,
        clock,
        presentableName
    )
    if (initDirectory != null && keyProvider != null) {
      processAvailableFiles(fileRepository, initDirectory!!, keyProvider!!)
    }
    return fileRepository
  }

  fun clock(clock: Clock): FileRepositoryBuilder<K> {
    this.clock = clock
    return this
  }

  fun presentableName(presentableName: String): FileRepositoryBuilder<K> {
    this.presentableName = presentableName
    return this
  }

  fun sweepPolicy(sweepPolicy: SweepPolicy<K>): FileRepositoryBuilder<K> {
    this.sweepPolicy = sweepPolicy
    return this
  }

  fun resourceProvider(resourceProvider: ResourceProvider<K, Path>): FileRepositoryBuilder<K> {
    this.resourceProvider = resourceProvider
    return this
  }

  fun addInitialFilesFrom(directory: Path, keyProvider: (Path) -> K?): FileRepositoryBuilder<K> {
    this.initDirectory = directory
    this.keyProvider = keyProvider
    return this
  }

  private fun <K> processAvailableFiles(fileRepository: FileRepository<K>,
                                        repositoryDir: Path,
                                        keyProvider: (Path) -> K?) {
    if (repositoryDir.exists()) {
      for (file in repositoryDir.listFiles()) {
        val key = keyProvider(file)
        if (key != null) {
          fileRepository.add(key, file)
        } else {
          file.deleteLogged()
        }
      }
    }
  }
}


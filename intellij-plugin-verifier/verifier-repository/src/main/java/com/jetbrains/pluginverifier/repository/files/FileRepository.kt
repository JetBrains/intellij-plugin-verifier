package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import com.jetbrains.pluginverifier.repository.downloader.SpaceWeight
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.repository.resources.ResourceRepository
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryImpl
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryResult
import java.nio.file.Path
import java.time.Clock

/**
 * File repository is the refinement of the
 * [resource repository] [ResourceRepository] for files.
 */
class FileRepository<K>(
    sweepPolicy: SweepPolicy<K>,
    resourceProvider: ResourceProvider<K, Path>,
    clock: Clock = Clock.systemUTC(),
    presentableName: String = "FileRepository"
) {
  private val resourceRepository = ResourceRepositoryImpl(
      sweepPolicy,
      clock,
      resourceProvider,
      SpaceWeight(SpaceAmount.ZERO_SPACE),
      { SpaceWeight(it.fileSize) },
      { path -> path.deleteLogged() },
      presentableName
  )

  /**
   * Provides the file by [key]. The file is returned from the
   * local cache or is provided by the [ResourceProvider] of this class's constructor.
   *
   * The possible results are represented as subclasses of [FileRepositoryResult].
   * If the file is found locally or successfully downloaded, a [file lock] [FileLock] is registered
   * for the file, so it will be protected against deletions by other threads.
   *
   *  This method is thread safe. In case several threads attempt to get the same file, only one
   * of them will download it while others will wait for the first to complete.
   *
   * @throws InterruptedException if the current thread has been interrupted while getting the file.
   */
  @Throws(InterruptedException::class)
  fun getFile(key: K): FileRepositoryResult = with(resourceRepository.get(key)) {
    when (this) {
      is ResourceRepositoryResult.Found -> FileRepositoryResult.Found(FileLockImpl(lockedResource))
      is ResourceRepositoryResult.NotFound -> FileRepositoryResult.NotFound(reason)
      is ResourceRepositoryResult.Failed -> FileRepositoryResult.Failed(reason, error)
    }
  }

  fun add(key: K, resource: Path): Boolean = resourceRepository.add(key, resource)

  fun remove(key: K): Boolean = resourceRepository.remove(key)

  fun removeAll() {
    resourceRepository.removeAll()
  }

  fun has(key: K): Boolean = resourceRepository.has(key)

  fun getAllExistingKeys(): Set<K> = resourceRepository.getAllExistingKeys()

  fun getAvailableFiles(): List<AvailableFile<K>> =
      resourceRepository.getAvailableResources().map {
        with(it) {
          AvailableFile(key, resourceInfo, usageStatistic, isLocked)
        }
      }

  fun cleanup() {
    resourceRepository.cleanup()
  }

}
/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.plugin.structure.base.utils.deleteLogged
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.pluginverifier.repository.cleanup.IdleSweepPolicy
import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.cleanup.SweepPolicy
import com.jetbrains.pluginverifier.repository.cleanup.fileSize
import com.jetbrains.pluginverifier.repository.provider.EmptyResourceProvider
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
class FileRepository<K : Any>(
  resourceProvider: ResourceProvider<K, Path> = EmptyResourceProvider(),
  sweepPolicy: SweepPolicy<K> = IdleSweepPolicy(),
  presentableName: String = "FileRepository",
  clock: Clock = Clock.systemUTC()
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

fun <K : Any> FileRepository<K>.addInitialFilesFrom(directory: Path, keyProvider: (Path) -> K?): FileRepository<K> {
  for (file in directory.listFiles()) {
    val key = keyProvider(file)
    if (key != null) {
      add(key, file)
    } else {
      file.deleteLogged()
    }
  }
  return this
}
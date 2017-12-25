package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.resources.ResourceRepository
import com.jetbrains.pluginverifier.repository.resources.ResourceRepositoryResult
import java.nio.file.Path

/**
 * File repository is the refinement of the
 * [resource repository] [ResourceRepository] for files.
 */
interface FileRepository<K> : ResourceRepository<Path, K> {

  /**
   * Provides the file by the specified key in a way
   * [specified] [get] by the implementation of this interface.
   *
   * The possible results are represented as subclasses of [FileRepositoryResult].
   * If the file is found locally or successfully downloaded, a [file lock] [FileLock] is registered
   * for the file, so it will be protected against deletions by other threads.
   */
  fun getFile(key: K): FileRepositoryResult = with(get(key)) {
    when (this) {
      is ResourceRepositoryResult.Found -> FileRepositoryResult.Found(FileLockImpl(lockedResource))
      is ResourceRepositoryResult.NotFound -> FileRepositoryResult.NotFound(reason)
      is ResourceRepositoryResult.Failed -> FileRepositoryResult.Failed(reason, error)
    }
  }

}
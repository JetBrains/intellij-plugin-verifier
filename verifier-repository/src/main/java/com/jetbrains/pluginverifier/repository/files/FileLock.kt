package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.nio.file.Path

/**
 * File lock is used to indicate that the [file]
 * is locked in the [file repository] [FileRepository]
 * and it cannot be removed until the lock is [released] [release]
 * by the lock owner.
 */
interface FileLock : ResourceLock<Path> {

  val file: Path

  override val resource: Path
    get() = file

}
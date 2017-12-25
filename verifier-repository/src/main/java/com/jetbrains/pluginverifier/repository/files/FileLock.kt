package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.resources.ResourceLock
import java.nio.file.Path
import java.time.Instant

/**
 * File lock is used to indicate that the [file]
 * is locked in the [file repository] [FileRepository],
 * thus it cannot be removed until the lock is [released] [release]
 * by the lock owner.
 */
abstract class FileLock(lockTime: Instant,
                        val file: Path,
                        val fileSize: SpaceAmount) : ResourceLock<Path>(lockTime, FileInfo(file, fileSize))
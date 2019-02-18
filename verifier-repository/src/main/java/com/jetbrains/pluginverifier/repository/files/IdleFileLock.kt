package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

/**
 * The [file lock] [FileLock] instance that doesn't require
 * the file to be locked in a [repository] [com.jetbrains.pluginverifier.repository.files.FileRepository].
 */
class IdleFileLock(file: Path) : FileLock(Instant.ofEpochMilli(0), Duration.ZERO, file, SpaceAmount.ZERO_SPACE) {
  override fun release() = Unit
}
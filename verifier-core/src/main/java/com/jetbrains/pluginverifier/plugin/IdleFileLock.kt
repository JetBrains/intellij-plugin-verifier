package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.nio.file.Path
import java.time.Instant

/**
 * @author Sergey Patrikeev
 */
class IdleFileLock(file: Path) : FileLock(Instant.ofEpochMilli(0), file, SpaceAmount.ZERO_SPACE) {
  override fun release() = Unit
}
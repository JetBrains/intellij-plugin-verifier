package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.files.FileLock
import java.nio.file.Path
import java.time.Instant

/**
 * @author Sergey Patrikeev
 */
data class IdleFileLock(override val file: Path) : FileLock {
  override val lockTime: Instant = Instant.ofEpochMilli(0)

  override fun release() = Unit
}
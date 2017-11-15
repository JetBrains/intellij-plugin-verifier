package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.FileLock
import java.io.File
import java.time.Instant

/**
 * @author Sergey Patrikeev
 */
data class IdleFileLock(override val file: File) : FileLock {
  override val lockTime: Instant = Instant.ofEpochMilli(0)

  override fun release() = Unit
}
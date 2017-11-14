package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.FileLock
import java.io.File

/**
 * @author Sergey Patrikeev
 */
data class IdleFileLock(override val file: File) : FileLock {
  override val lockTime: Long = 0

  override fun release() = Unit
}
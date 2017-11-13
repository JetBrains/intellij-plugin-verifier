package com.jetbrains.pluginverifier.plugin

import com.jetbrains.pluginverifier.repository.FileLock
import java.io.File

/**
 * @author Sergey Patrikeev
 */
data class IdleFileLock(private val backedFile: File) : FileLock() {
  override val lockTime: Long = 0

  override fun release() = Unit

  override fun getFile(): File = backedFile
}
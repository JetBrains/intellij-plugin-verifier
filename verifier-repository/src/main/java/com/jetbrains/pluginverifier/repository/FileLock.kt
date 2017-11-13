package com.jetbrains.pluginverifier.repository

import java.io.Closeable
import java.io.File

abstract class FileLock : Closeable {

  abstract val lockTime: Long

  abstract fun getFile(): File

  abstract fun release()

  override fun close() = release()
}
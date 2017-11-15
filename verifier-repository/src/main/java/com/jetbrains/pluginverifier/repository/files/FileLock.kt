package com.jetbrains.pluginverifier.repository.files

import java.io.Closeable
import java.io.File
import java.time.Instant

interface FileLock : Closeable {

  val lockTime: Instant

  val file: File

  fun release()

  override fun close() = release()
}
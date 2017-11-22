package com.jetbrains.pluginverifier.repository.files

import java.io.Closeable
import java.nio.file.Path
import java.time.Instant

interface FileLock : Closeable {

  val lockTime: Instant

  val file: Path

  fun release()

  override fun close() = release()
}
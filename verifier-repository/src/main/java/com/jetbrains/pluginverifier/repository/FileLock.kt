package com.jetbrains.pluginverifier.repository

import java.io.Closeable
import java.io.File
import java.time.Instant

//todo: move it
interface FileLock : Closeable {

  val lockTime: Instant

  val file: File

  fun release()

  override fun close() = release()
}
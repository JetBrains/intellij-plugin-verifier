package com.jetbrains.pluginverifier.repository

import java.io.Closeable
import java.io.File

//todo: move it
interface FileLock : Closeable {

  val lockTime: Long

  val file: File

  fun release()

  override fun close() = release()
}
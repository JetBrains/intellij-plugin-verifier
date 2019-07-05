package com.jetbrains.pluginverifier.reporting.common

import com.jetbrains.plugin.structure.base.utils.create
import com.jetbrains.plugin.structure.base.utils.rethrowIfInterrupted
import com.jetbrains.pluginverifier.reporting.Reporter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FileReporter<in T>(
    private val file: Path,
    private val lineProvider: (T) -> String = { it.toString() }
) : Reporter<T> {

  private var fileWriter: Writer? = null

  private val lock: ReentrantLock = ReentrantLock(true)

  private var isClosed: Boolean = false

  private fun openFileWriter(): BufferedWriter? = try {
    Files.newBufferedWriter(file.create())
  } catch (e: Exception) {
    e.rethrowIfInterrupted()
    ERROR_LOGGER.error("Failed to open file writer for $file", e)
    null
  }

  @Synchronized
  override fun report(t: T) {
    val line = lineProvider(t)
    lock.withLock {
      if (!isClosed) {
        if (fileWriter == null) {
          fileWriter = openFileWriter()
        }
        try {
          fileWriter?.appendln(line)
        } catch (e: Exception) {
          e.rethrowIfInterrupted()
          isClosed = true
          ERROR_LOGGER.error("Failed to report into $file", e)
        }
      }
    }
  }

  override fun close() {
    lock.withLock {
      if (!isClosed) {
        isClosed = true
        try {
          fileWriter?.close()
        } catch (e: Exception) {
          e.rethrowIfInterrupted()
          ERROR_LOGGER.error("Failed to close file writer for $file", e)
        }
      }
    }
  }

  private companion object {
    val ERROR_LOGGER: Logger = LoggerFactory.getLogger(FileReporter::class.java)
  }
}
package com.jetbrains.pluginverifier.logging.loggers

import java.io.File

class FileLogger(private val file: File) : Logger {

  override fun createSubLogger(name: String): FileLogger {
    val subFile = File(file, name)
    return FileLogger(subFile)
  }

  override fun info(message: String, e: Throwable?) {

  }

  override fun warn(message: String, e: Throwable?) {

  }

  override fun error(message: String, e: Throwable?) {

    org.slf4j.Logger
  }

  override fun close() {
  }
}
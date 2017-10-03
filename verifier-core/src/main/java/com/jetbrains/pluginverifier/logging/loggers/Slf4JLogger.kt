package com.jetbrains.pluginverifier.logging.loggers

import org.slf4j.LoggerFactory

class Slf4JLogger(private val slf4jLog: org.slf4j.Logger) : Logger {
  override fun close() = Unit

  override fun createSubLogger(name: String): Logger =
      Slf4JLogger(LoggerFactory.getLogger(slf4jLog.name + "." + name))

  override fun warn(message: String, e: Throwable?) {
    slf4jLog.warn(message, e)
  }

  override fun error(message: String, e: Throwable?) {
    slf4jLog.error(message, e)
  }

  override fun info(message: String, e: Throwable?) {
    slf4jLog.info(message, e)
  }
}
package com.jetbrains.plugin.structure.base.logging

class Slf4jLoggerAdapter(private val slf4jLogger: org.slf4j.Logger) : Logger {
  override fun error(message: String) = error(message, null)

  override fun info(message: String) = info(message, null)

  override fun warn(message: String) = warn(message, null)

  override fun error(message: String, e: Throwable?) {
    slf4jLogger.error(message, e)
  }

  override fun info(message: String, e: Throwable?) {
    slf4jLogger.info(message, e)
  }

  override fun warn(message: String, e: Throwable?) {
    slf4jLogger.warn(message, e)
  }
}
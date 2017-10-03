package com.jetbrains.pluginverifier.logging.loggers

import java.io.Closeable

/**
 * @author Sergey Patrikeev
 */
interface Logger : Closeable {
  fun info(message: String, e: Throwable?)

  fun info(message: String) = info(message, null)

  fun warn(message: String, e: Throwable?)

  fun warn(message: String) = warn(message, null)

  fun error(message: String, e: Throwable?)

  fun error(message: String) = error(message, null)

  fun createSubLogger(name: String): Logger

}
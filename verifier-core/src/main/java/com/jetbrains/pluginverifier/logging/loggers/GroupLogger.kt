package com.jetbrains.pluginverifier.logging.loggers

import com.jetbrains.pluginverifier.misc.closeLogged

/**
 * @author Sergey Patrikeev
 */
class GroupLogger(private val loggers: List<Logger>) : Logger {
  override fun createSubLogger(name: String): Logger = GroupLogger(loggers.map { it.createSubLogger(name) })

  override fun info(message: String, e: Throwable?) {
    loggers.forEach { it.info(message, e) }
  }

  override fun warn(message: String, e: Throwable?) {
    loggers.forEach { it.warn(message, e) }
  }

  override fun error(message: String, e: Throwable?) {
    loggers.forEach { it.error(message, e) }
  }

  override fun close() {
    loggers.forEach { it.closeLogged() }
  }

}

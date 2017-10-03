package com.jetbrains.plugin.structure.base.logging

import org.slf4j.LoggerFactory

/**
 * @author Sergey Patrikeev
 */
object LoggerFactory {
  fun createDefaultLogger(name: String): Logger = Slf4jLoggerAdapter(LoggerFactory.getLogger(name))

  fun createDefaultLogger(`class`: Class<*>): Logger = Slf4jLoggerAdapter(LoggerFactory.getLogger(`class`))
}
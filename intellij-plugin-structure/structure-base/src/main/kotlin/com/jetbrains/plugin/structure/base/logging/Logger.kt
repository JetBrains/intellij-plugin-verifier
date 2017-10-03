package com.jetbrains.plugin.structure.base.logging

/**
 * @author Sergey Patrikeev
 */
interface Logger {
  fun error(message: String)

  fun error(message: String, e: Throwable?)

  fun info(message: String)

  fun info(message: String, e: Throwable? = null)

  fun warn(message: String)

  fun warn(message: String, e: Throwable? = null)
}
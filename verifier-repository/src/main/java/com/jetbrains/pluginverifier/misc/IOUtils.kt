package com.jetbrains.pluginverifier.misc

import org.slf4j.LoggerFactory
import java.io.Closeable

private val LOG = LoggerFactory.getLogger("IOUtils")

fun <T : Closeable?> T.closeLogged() {
  try {
    this?.close()
  } catch (ie: InterruptedException) {
    Thread.currentThread().interrupt()
    LOG.error("Interrupted exception on closing $this", ie)
  } catch (e: Exception) {
    LOG.error("Unable to close $this", e)
  }
}

inline fun <T : Closeable?, R> List<T>.closeOnException(block: (List<T>) -> R): R {
  try {
    return block(this)
  } catch (e: Throwable) {
    this.forEach { t: T -> t?.closeLogged() }
    throw e
  }
}

/**
 * Executes the given [block] of code on `this` closeable instance
 * and returns the execution result.
 *
 * If an exception is thrown, `this` will be closed with logging and
 * the exception will be propagated.
 */
inline fun <T : Closeable?, R> T.closeOnException(block: (T) -> R): R {
  try {
    return block(this)
  } catch (e: Throwable) {
    this?.closeLogged()
    throw e
  }
}

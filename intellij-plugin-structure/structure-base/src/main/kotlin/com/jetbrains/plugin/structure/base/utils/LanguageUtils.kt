package com.jetbrains.plugin.structure.base.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException

private val logger: Logger = LoggerFactory.getLogger("LanguageUtils")

/**
 * Checks whether the current thread has been interrupted.
 * Clears the *interrupted status* and throws [InterruptedException]
 * if it is the case.
 */
@Throws(InterruptedException::class)
fun checkIfInterrupted() {
  if (Thread.interrupted()) {
    throw InterruptedException()
  }
}

fun <T : Closeable?> T.closeLogged() {
  try {
    this?.close()
  } catch (e: Exception) {
    logger.error("Unable to close $this", e)
  }
}

inline fun <T : Closeable?, R> T.closeOnException(block: (T) -> R): R {
  try {
    return block(this)
  } catch (e: Throwable) {
    this?.closeLogged()
    throw e
  }
}

/**
 * Closes multiple resources and throws an exception with all failed-to-close causes
 * set as suppressed exceptions, if any.
 */
fun List<Closeable>.closeAll() {
  val exceptions = mapNotNull {
    try {
      it.close()
      null
    } catch (e: Exception) {
      e
    }
  }
  if (exceptions.isNotEmpty()) {
    val closeException = IOException("Exceptions while closing multiple resources")
    exceptions.forEach { closeException.addSuppressed(it) }
    throw closeException
  }
}

fun <T> Iterator<T>.toList() = asSequence().toList()

fun <T> Iterator<T>.toSet() = asSequence().toSet()
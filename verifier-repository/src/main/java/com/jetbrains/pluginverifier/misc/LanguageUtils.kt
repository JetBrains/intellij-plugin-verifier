package com.jetbrains.pluginverifier.misc

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
private val LOG = LoggerFactory.getLogger("LanguageUtils")

/**
 * Creates a Guava multimap using the input map.
 */
fun <K, V> Map<K, Iterable<V>>.multimapFromMap(): Multimap<K, V> {
  val result = ArrayListMultimap.create<K, V>()
  for ((key, values) in this) {
    result.putAll(key, values)
  }
  return result
}

fun <T : Closeable?> T.closeLogged() {
  try {
    this?.close()
  } catch(e: Exception) {
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

inline fun <T : Closeable?, R> T.closeOnException(block: (T) -> R): R {
  try {
    return block(this)
  } catch (e: Throwable) {
    this?.closeLogged()
    throw e
  }
}

fun String.toSystemIndependentName() = replace('\\', '/')

fun File.create(): File {
  if (this.parentFile != null) {
    FileUtils.forceMkdir(this.parentFile)
  }
  this.createNewFile()
  return this
}

fun File.createDir(): File {
  if (!isDirectory) {
    FileUtils.forceMkdir(this)
    if (!isDirectory) {
      throw IOException("Failed to create directory ${this}")
    }
  }
  return this
}

inline fun <R> withDebug(logger: Logger, taskName: String, block: () -> R): R {
  val startTime = System.currentTimeMillis()
  logger.debug(taskName + " is starting")
  try {
    return block()
  } finally {
    val elapsedTime = System.currentTimeMillis() - startTime
    logger.debug(taskName + " is finished in ${elapsedTime / 1000} seconds")
  }
}

fun File.deleteLogged(): Boolean {
  try {
    if (exists()) {
      FileUtils.forceDelete(this)
    }
    return true
  } catch(e: Exception) {
    LOG.error("Unable to delete $this", e)
    return false
  }
}

fun String.pluralizeWithNumber(times: Int): String = "$times " + this.pluralize(times)

fun String.pluralize(times: Int): String {
  if (times < 0) throw IllegalArgumentException("Negative value")
  if (times <= 1) {
    return this
  } else {
    if (this.endsWith("y")) {
      return this.dropLast(1) + "ies"
    }
    return this + "s"
  }
}

fun impossible(): Nothing = throw AssertionError("Impossible")

fun Long.bytesToMegabytes(digits: Int = 2): String = "%.${digits}f".format(this.toDouble() / FileUtils.ONE_MB)

fun Long.bytesToGigabytes(digits: Int = 3): String = "%.${digits}f".format(this.toDouble() / FileUtils.ONE_GB)
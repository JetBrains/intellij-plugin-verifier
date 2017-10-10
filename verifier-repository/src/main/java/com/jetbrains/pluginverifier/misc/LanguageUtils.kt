package com.jetbrains.pluginverifier.misc

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.apache.commons.io.FileUtils
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

inline fun <T> buildList(builderAction: MutableList<T>.() -> Unit): List<T> = arrayListOf<T>().apply(builderAction)

fun File.createDir(): File {
  if (!isDirectory) {
    FileUtils.forceMkdir(this)
    if (!isDirectory) {
      throw IOException("Failed to create directory ${this}")
    }
  }
  return this
}

fun checkIfInterrupted() {
  if (Thread.currentThread().isInterrupted) {
    throw InterruptedException()
  }
}

fun <T> T?.singletonOrEmpty(): List<T> = if (this == null) emptyList() else listOf(this)

fun File.deleteLogged(): Boolean = try {
  if (exists()) {
    FileUtils.forceDelete(this)
  }
  true
} catch (e: Exception) {
  LOG.error("Unable to delete $this", e)
  false
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

fun <T> List<T>.listEndsWith(vararg ending: T): Boolean {
  if (ending.isEmpty()) {
    return true
  }
  if (size < ending.size) {
    return false
  }
  if (size == ending.size) {
    return this == ending.toList()
  }
  return ending.indices.all { index -> ending[index] == this[size - ending.size + index] }
}

fun String.replaceInvalidFileNameCharacters(): String = replace(Regex("[^a-zA-Z0-9.#\\-() ]"), "_")

fun impossible(): Nothing = throw AssertionError("Impossible")

fun Long.bytesToMegabytes(digits: Int = 2): String = "%.${digits}f".format(this.toDouble() / FileUtils.ONE_MB)

fun Long.bytesToGigabytes(digits: Int = 3): String = "%.${digits}f".format(this.toDouble() / FileUtils.ONE_GB)
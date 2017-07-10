package com.jetbrains.pluginverifier.misc

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Closeable
import java.io.File
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

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

fun File.create(): File {
  if (this.parentFile != null) {
    FileUtils.forceMkdir(this.parentFile)
  }
  this.createNewFile()
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

fun <T> Call<T>.executeSuccessfully(): Response<T> {
  val serverUrl = getServerUrl()

  val callResponse = AtomicReference<Response<T>?>(null)
  val callError = AtomicReference<Throwable?>(null)
  val finished = AtomicBoolean()
  this.enqueue(object : Callback<T> {
    override fun onResponse(call: Call<T>, response: Response<T>) {
      callResponse.set(response)
      finished.set(true)
    }

    override fun onFailure(call: Call<T>, error: Throwable) {
      callError.set(error)
      finished.set(true)
    }
  })

  while (!finished.get()) {
    if (Thread.currentThread().isInterrupted) {
      this.cancel()
      throw InterruptedException()
    }
    Thread.sleep(100)
  }

  return getAppropriateResponse(serverUrl, callResponse.get(), callError.get())
}

private fun <T> getAppropriateResponse(serverUrl: String, response: Response<T>?, error: Throwable?): Response<T> {
  if (response != null) {
    if (response.isSuccessful) {
      return response
    }
    if (response.code() == 404) {
      throw RuntimeException("Not found 404")
    }
    if (response.code() == 500) {
      throw RuntimeException("Server $serverUrl has faced problems 500")
    }
    val message = response.errorBody().string().take(100)
    throw RuntimeException("Server $serverUrl response = ${response.code()}: $message")
  }
  if (error != null) {
    val errorMessage = error.message
    throw RuntimeException("Unable to communicate with $serverUrl: $errorMessage", error)
  }
  throw RuntimeException("Unable to connect $serverUrl")
}

private fun <T> Call<T>.getServerUrl(): String = "${request().url().host()}:${request().url().port()}"

fun Long.bytesToMegabytes(): Double = this.toDouble() / FileUtils.ONE_MB

fun Long.bytesToGigabytes(): Double = this.toDouble() / FileUtils.ONE_GB
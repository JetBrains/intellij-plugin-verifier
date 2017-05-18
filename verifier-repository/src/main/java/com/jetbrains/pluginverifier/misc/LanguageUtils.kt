package com.jetbrains.pluginverifier.misc

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Response
import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
private val LOG = LoggerFactory.getLogger("LanguageUtils")

fun <T> T?.notNullize(default: T) = if (this == null) default else this

fun Closeable.closeLogged() {
  try {
    this.close()
  } catch(e: Exception) {
    LOG.error("Unable to close $this", e)
  }
}

fun File.create(): File {
  if (this.parentFile != null) {
    FileUtils.forceMkdir(this.parentFile)
  }
  this.createNewFile()
  return this
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

fun String.pluralize(times: Int): String {
  if (times < 0) throw IllegalArgumentException("Negative value")
  if (times == 0) return ""
  if (times == 1) {
    return this
  } else {
    if (this.endsWith("y")) {
      return this.dropLast(1) + "ies"
    }
    return this + "s"
  }
}

fun <T> Call<T>.executeSuccessfully(): Response<T> {
  val server = "${this.request().url().host()}:${this.request().url().port()}"
  val response: Response<T>?
  try {
    response = this.execute()
  } catch(e: IOException) {
    throw RuntimeException("The server $server is not available", e)
  }
  if (response.isSuccessful) {
    return response
  }
  if (response.code() == 500) {
    throw RuntimeException("The server $server has faced unexpected problems (500 Internal Server Error)")
  }
  val errorString = response.errorBody().string()
  throw RuntimeException("The response status code is ${response.code()}: ${if (errorString.length > 100) "(too long error body...)" else errorString}")
}
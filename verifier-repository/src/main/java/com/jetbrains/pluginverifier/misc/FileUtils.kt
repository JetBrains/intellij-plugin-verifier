package com.jetbrains.pluginverifier.misc

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

private val LOG = LoggerFactory.getLogger("FileUtils")

fun String.toSystemIndependentName() = replace('\\', '/')

fun String.replaceInvalidFileNameCharacters(): String = replace(Regex("[^a-zA-Z0-9.#\\-() ]"), "_")

fun File.forceDeleteIfExists() {
  if (exists()) {
    FileUtils.forceDelete(this)
  }
}

fun File.deleteLogged(): Boolean = try {
  forceDeleteIfExists()
  true
} catch (e: Exception) {
  LOG.error("Unable to delete $this", e)
  false
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

fun File.create(): File {
  if (this.parentFile != null) {
    FileUtils.forceMkdir(this.parentFile)
  }
  this.createNewFile()
  return this
}


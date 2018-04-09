package com.jetbrains.pluginverifier.misc

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

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

fun Path.writeText(text: String) {
  toFile().writeText(text)
}

fun Path.readText() = toFile().readText()

fun Path.readLines() = toFile().readLines()

fun Path.writeBytes(bytes: ByteArray) {
  toFile().writeBytes(bytes)
}

fun Path.createDir(): Path {
  toFile().createDir()
  return this
}

fun Path.create(): Path {
  toFile().create()
  return this
}

fun Path.forceDeleteIfExists() {
  toFile().forceDeleteIfExists()
}

fun Path.deleteLogged() {
  toFile().deleteLogged()
}

fun Path.exists(): Boolean = Files.exists(this)

fun Path.listFiles(): List<Path> = Files.list(this).collect(Collectors.toList())

val Path.isDirectory
  get() = Files.isDirectory(this)

val Path.nameWithoutExtension
  get() = toFile().nameWithoutExtension

val Path.simpleName
  get() = fileName.toString()

val Path.extension
  get() = toFile().extension

val Path.length
  get() = toFile().length()
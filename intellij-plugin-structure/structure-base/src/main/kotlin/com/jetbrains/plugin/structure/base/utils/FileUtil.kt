package com.jetbrains.plugin.structure.base.utils

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

private val LOG = LoggerFactory.getLogger("structure.FileUtil")

fun File.isZip(): Boolean = this.hasExtension("zip")

fun File.isJar(): Boolean = this.hasExtension("jar")

fun File.hasExtension(expected: String) =
    isFile && expected == extension

fun File.forceDeleteIfExists() {
  if (exists()) {
    FileUtils.forceDelete(this)
  }
}

fun File.listRecursivelyAllFilesWithName(name: String): Collection<File> =
    FileUtils.listFiles(this, NameFileFilter(name), TrueFileFilter.TRUE)

fun File.listRecursivelyAllFilesWithExtension(extension: String): Collection<File> =
    FileUtils.listFiles(this, WildcardFileFilter("*.$extension"), TrueFileFilter.TRUE)

fun File.deleteLogged(): Boolean = try {
  forceDeleteIfExists()
  true
} catch (ie: InterruptedException) {
  Thread.currentThread().interrupt()
  LOG.info("Cannot delete file because of interruption:  $this")
  false
} catch (e: Exception) {
  LOG.error("Unable to delete $this", e)
  false
}

fun String.toSystemIndependentName() = replace('\\', '/')

fun String.replaceInvalidFileNameCharacters(): String = replace(Regex("[^a-zA-Z0-9.#\\-() ]"), "_")

fun File.createDir(): File {
  if (!isDirectory) {
    FileUtils.forceMkdir(this)
    if (!isDirectory) {
      throw IOException("Failed to create directory $this")
    }
  }
  return this
}

fun File.createParentDirs() {
  parentFile?.createDir()
}

fun File.create(): File {
  if (this.parentFile != null) {
    FileUtils.forceMkdir(this.parentFile)
  }
  this.createNewFile()
  return this
}

fun Path.writeText(text: String) {
  toFile().create().writeText(text)
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

val Path.isDirectory: Boolean
  get() = Files.isDirectory(this)

val Path.simpleName: String
  get() = fileName.toString()

val Path.nameWithoutExtension: String
  get() = simpleName.substringBeforeLast(".")

val Path.extension: String
  get() = simpleName.substringAfterLast(".", "")

val Path.length: Long
  get() = Files.size(this)

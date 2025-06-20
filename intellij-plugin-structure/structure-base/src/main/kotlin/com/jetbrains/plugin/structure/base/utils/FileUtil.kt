/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.telemetry.UNKNOWN_SIZE
import org.apache.commons.io.FileUtils.ONE_GB
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.math.BigDecimal
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import kotlin.streams.toList

private val LOG = LoggerFactory.getLogger("structure.FileUtil")

typealias Bytes = Long

internal val ONE_GB_BD = BigDecimal(ONE_GB)
internal val FIVE_GB = BigDecimal("5").multiply(ONE_GB_BD).toLong()

fun Path.isZip(): Boolean = this.hasExtension("zip")

fun Path.isJar(): Boolean = this.hasExtension("jar")

fun Path.hasExtension(expected: String) =
  Files.isRegularFile(this) && expected == extension

fun Path.listRecursivelyAllFilesWithExtension(extension: String) =
  Files.walk(this, FileVisitOption.FOLLOW_LINKS)
    .use { stream -> stream.filter { it.toString().endsWith(".${extension}") }.toList() }

fun String.withPathSeparatorOf(path: Path) = replace('\\', '/').replace("/", path.fileSystem.separator)
fun String.withZipFsSeparator() = replace('\\', '/')

fun String.toSystemIndependentName() = replace('\\', '/')
fun String.toSystemDependentName() = replace("/", FileSystems.getDefault().separator)

fun String.replaceInvalidFileNameCharacters(): String = replace(Regex("[^a-zA-Z0-9.#\\-() ]"), "_")

fun Path.inputStream(): InputStream = Files.newInputStream(this)

fun Path.bufferedInputStream() = Files.newInputStream(this).buffered()

fun Path.outputStream(): OutputStream = Files.newOutputStream(this)

fun Path.writeText(text: String, charset: Charset = Charsets.UTF_8) = this.writeBytes(text.toByteArray(charset))

fun Path.readText(charset: Charset = Charsets.UTF_8) = String(Files.readAllBytes(this), charset)

fun Path.readLines(charset: Charset = Charsets.UTF_8): List<String> {
  val result = ArrayList<String>()
  forEachLine(charset) { result.add(it); }
  return result
}

fun Path.forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit) {
  // Note: close is called at forEachLine
  BufferedReader(InputStreamReader(Files.newInputStream(this), charset)).forEachLine(action)
}

fun Path.readBytes(): ByteArray = Files.readAllBytes(this)

fun Path.writeBytes(bytes: ByteArray) {
  Files.write(this, bytes)
}

fun Path.createDir(): Path {
  Files.createDirectories(this)
  return this
}

fun Path.create(): Path {
  if (this.parent != null) {
    Files.createDirectories(this.parent)
  }
  Files.createFile(this)
  return this
}

fun Path.createParentDirs() {
  parent?.createDir()
}

fun Path.forceDeleteIfExists() {
  if (Files.exists(this)) {
    if (Files.isDirectory(this)) {
      this.forceRemoveDirectory()
    } else {
      Files.delete(this)
    }
  }
}

fun Path.deleteLogged() = try {
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

fun Path.exists(): Boolean = Files.exists(this)

fun Path.listFiles(): List<Path> {
  if (!this.isDirectory) {
    return emptyList()
  }
  return Files.list(this).use { it.collect(Collectors.toList()) }
}

fun Path.listJars(): List<Path> = listFiles().filter { it.isJar() }

fun Path.listAllFiles() =
  Files.walk(this).use { stream -> stream.filter { it.isFile }.map { this.relativize(it) }.toList() }

fun Path.deleteQuietly(): Boolean {
  return try {
    if (this.isDirectory) {
      this.forceRemoveDirectory()
    } else {
      Files.delete(this)
    }
    true
  } catch (ignored: Exception) {
    false
  }
}

fun Path.forceRemoveDirectory() {
  Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
    @Throws(IOException::class)
    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
      Files.delete(file)
      return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
      Files.delete(dir)
      return FileVisitResult.CONTINUE
    }
  })
}

val Path.isDirectory: Boolean
  get() = Files.isDirectory(this)

val Path.isFile: Boolean
  get() = Files.isRegularFile(this)

val Path.simpleName: String
  get() = (fileName ?: "").toString()

val Path.nameWithoutExtension: String
  get() = simpleName.substringBeforeLast(".")

val Path.extension: String
  get() = simpleName.substringAfterLast(".", "")

val Path.length: Long
  get() = Files.size(this)

val Path.pluginSize: Bytes
  get() = if (isZip() || isJar()) Files.size(this) else UNKNOWN_SIZE

val Path.description: String
  get() {
    return if (fileSystem == FileSystems.getDefault()) {
      toString()
    } else {
      "$fileSystem!$this"
    }
  }

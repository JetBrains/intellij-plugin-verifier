package com.jetbrains.pluginverifier.misc

import com.jetbrains.plugin.structure.base.utils.deleteLogged
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

fun String.toSystemIndependentName() = replace('\\', '/')

fun String.replaceInvalidFileNameCharacters(): String = replace(Regex("[^a-zA-Z0-9.#\\-() ]"), "_")

fun File.forceDeleteIfExists() {
  if (exists()) {
    FileUtils.forceDelete(this)
  }
}

fun File.createDir(): File {
  if (!isDirectory) {
    FileUtils.forceMkdir(this)
    if (!isDirectory) {
      throw IOException("Failed to create directory $this")
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

val Path.isDirectory
  get() = Files.isDirectory(this)

val Path.simpleName: String
  get() = fileName.toString()

val Path.nameWithoutExtension: String
  get() = simpleName.substringBeforeLast(".")

val Path.extension: String
  get() = simpleName.substringAfterLast(".", "")

val Path.length: Long
  get() = Files.size(this)

/**
 * If the [directory] contains a single directory,
 * that directory will be truncated and all its
 * content will be moved one level up.
 */
fun stripTopLevelDirectory(directory: Path) {
  val entries = directory.listFiles()
  if (entries.size != 1) {
    return
  }

  val single = entries.single()
  if (!single.isDirectory) {
    return
  }

  val contents = single.listFiles()

  var conflict: Path? = null
  for (from in contents) {
    if (from.simpleName == single.simpleName) {
      conflict = from
      continue
    }

    val to = directory.resolve(from.simpleName)
    Files.move(from, to)
  }

  if (conflict != null) {
    //Create a unique temporary name from the set of files.
    //This name will be used as a destination of a conflicting name.
    val uniqueTempName = contents.map { it.simpleName }.sorted().last() + ".temp"
    val tempDestination = directory.resolve(uniqueTempName)

    //Move conflict to unique location.
    require(!tempDestination.exists())
    Files.move(conflict, tempDestination)

    //Delete empty single
    require(single.listFiles().isEmpty())
    single.deleteLogged()

    Files.move(tempDestination, single)
  } else {
    single.deleteLogged()
  }
}
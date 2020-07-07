/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.decompress

import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.simpleName
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipCompressor(outputFile: Path) : Closeable {

  private val outStream = ZipOutputStream(BufferedOutputStream(Files.newOutputStream(outputFile)))

  @Throws(IOException::class)
  fun addDirectory(directory: Path) {
    directory.listFiles().forEach { child ->
      val name = child.simpleName
      if (child.isDirectory) {
        addRecursively(name, child)
      } else {
        addFile(name, child)
      }
    }
  }

  private fun addFile(entryName: String, file: Path) {
    Files.newInputStream(file).use { source ->
      writeFileEntry(entryName, source, Files.size(file), Files.getLastModifiedTime(file).toMillis())
    }
  }

  private fun addRecursively(prefix: String, directory: Path) {
    if (prefix.isNotEmpty()) {
      writeDirectoryEntry(prefix, Files.getLastModifiedTime(directory).toMillis())
    }
    val children = directory.listFiles()
    children.forEach { child ->
      val name = if (prefix.isEmpty()) child.simpleName else "$prefix/${child.simpleName}"
      if (child.isDirectory) {
        addRecursively(name, child)
      } else {
        addFile(name, child)
      }
    }
  }

  private fun writeDirectoryEntry(name: String, timestamp: Long) {
    val e = ZipEntry("$name/")
    e.method = ZipEntry.STORED
    e.size = 0
    e.crc = 0
    e.time = timestamp
    outStream.putNextEntry(e)
    outStream.closeEntry()
  }

  private fun writeFileEntry(name: String?, source: InputStream, length: Long, timestamp: Long) {
    val e = ZipEntry(name)
    if (length == 0L) {
      e.method = ZipEntry.STORED
      e.size = 0
      e.crc = 0
    }
    e.time = timestamp
    outStream.putNextEntry(e)
    source.copyTo(outStream, 4096)
    outStream.closeEntry()
  }

  override fun close() {
    outStream.close()
  }
}
// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.plugin.structure.base.decompress

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipCompressor(outputFile: File) : Closeable {

  private val outStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))

  @Throws(IOException::class)
  fun addDirectory(directory: File) {
    val children = directory.listFiles()
    if (children != null) {
      for (child in children) {
        val name = child.name
        if (child.isDirectory) {
          addRecursively(name, child)
        } else {
          addFile(name, child)
        }
      }
    }
  }

  private fun addFile(entryName: String, file: File) {
    FileInputStream(file).use { source -> writeFileEntry(entryName, source, file.length(), file.lastModified()) }
  }

  private fun addRecursively(prefix: String, directory: File) {
    if (prefix.isNotEmpty()) {
      writeDirectoryEntry(prefix, directory.lastModified())
    }
    val children = directory.listFiles()
    if (children != null) {
      for (child in children) {
        val name = if (prefix.isEmpty()) child.name else prefix + '/' + child.name
        if (child.isDirectory) {
          addRecursively(name, child)
        } else {
          addFile(name, child)
        }
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
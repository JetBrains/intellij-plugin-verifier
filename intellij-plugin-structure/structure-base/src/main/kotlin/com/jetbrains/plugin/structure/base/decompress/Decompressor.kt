// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.plugin.structure.base.decompress

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.createParentDirs
import org.apache.commons.io.input.BoundedInputStream
import org.apache.commons.io.input.CountingInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

internal sealed class Decompressor(private val outputSizeLimit: Long?) {

  fun extract(outputDir: File) {
    openStream()
    try {
      val actualSizeLimit = outputSizeLimit ?: Long.MAX_VALUE
      var remainingSize = actualSizeLimit
      loop@ while (true) {
        val entry = nextEntry() ?: break
        val outputFile = getEntryFile(outputDir, entry)
        when (entry.type) {
          Type.DIR -> {
            outputFile.createDir()
          }
          Type.FILE -> {
            val inputStream = nextEntryStream() ?: continue@loop
            inputStream.use {
              val boundedStream = BoundedInputStream(inputStream, remainingSize + 1)
              val countingStream = CountingInputStream(boundedStream)

              outputFile.createParentDirs()
              outputFile.outputStream().buffered().use { countingStream.copyTo(it) }

              remainingSize -= countingStream.byteCount
              if (remainingSize < 0) {
                throw DecompressorSizeLimitExceededException(actualSizeLimit)
              }
            }
          }
          Type.SYMLINK -> throw IOException("Symlinks are not allowed")
        }
      }
    } finally {
      closeStream()
    }
  }

  enum class Type { FILE, DIR, SYMLINK }

  class Entry(val name: String, val type: Type)

  abstract fun openStream()

  abstract fun nextEntry(): Entry?

  abstract fun nextEntryStream(): InputStream?

  abstract fun closeStream()

}

private fun getEntryFile(outputDir: File, entry: Decompressor.Entry): File {
  val entryName = entry.name
  if (entryName.contains("..") && entryName.replace("\\", "/").split("/").any { it.contains("..") }) {
    throw IOException("Invalid entry name: $entryName")
  }
  return outputDir.resolve(entryName)
}

internal class ZipDecompressor(private val source: File, sizeLimit: Long?) : Decompressor(sizeLimit) {

  private lateinit var zipFile: ZipFile

  private lateinit var entries: Enumeration<out ZipEntry>

  private var entry: ZipEntry? = null

  override fun openStream() {
    zipFile = ZipFile(source)
    entries = zipFile.entries()
  }

  override fun nextEntry(): Entry? {
    entry = if (entries.hasMoreElements()) {
      entries.nextElement()
    } else {
      null
    }

    return if (entry == null) {
      null
    } else {
      val type = if (entry!!.isDirectory) {
        Type.DIR
      } else {
        Type.FILE
      }
      Entry(entry!!.name, type)
    }
  }

  override fun nextEntryStream(): InputStream? =
      zipFile.getInputStream(this.entry)

  override fun closeStream() {
    zipFile.close()
  }
}

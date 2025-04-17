/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.decompress

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.createParentDirs
import com.jetbrains.plugin.structure.base.utils.inputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.io.input.BoundedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream

internal sealed class Decompressor(private val outputSizeLimit: Long?) {

  companion object {
    const val FILE_NAME_LENGTH_LIMIT = 255
  }

  fun extract(outputDir: Path) {
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
            val entryStream = nextEntryStream() ?: continue@loop
            try {
              val countingStream = BoundedInputStream.builder()
                .setInputStream(entryStream)
                .setMaxCount(remainingSize + 1)
                .setPropagateClose(false)
                .get()


              outputFile.createParentDirs()
              Files.copy(countingStream, outputFile)
              remainingSize -= countingStream.count
              if (remainingSize < 0) {
                throw DecompressorSizeLimitExceededException(actualSizeLimit)
              }
            } finally {
              closeNextEntryStream(entryStream)
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

  abstract fun closeNextEntryStream(entryStream: InputStream)

  abstract fun closeStream()

}

private fun getEntryFile(outputDir: Path, entry: Decompressor.Entry): Path {
  val independentEntryName = entry.name.replace("\\", "/")
  val parts = independentEntryName.split("/")
  if (parts.any { it.contains("..") }) {
    throw IOException("Invalid relative entry name: ${entry.name}")
  }
  if (parts.any { it.length > Decompressor.FILE_NAME_LENGTH_LIMIT }) {
    throw IOException("Entry name is too long: ${entry.name}")
  }
  return outputDir.resolve(independentEntryName)
}

internal class ZipDecompressor(private val zipFile: Path, sizeLimit: Long?) : Decompressor(sizeLimit) {
  private lateinit var stream: ZipInputStream

  override fun openStream() {
    stream = ZipInputStream(zipFile.inputStream())
  }

  override fun nextEntry(): Entry? {
    val nextEntry = stream.nextEntry ?: return null
    val type = when {
      nextEntry.isDirectory -> Type.DIR
      else -> Type.FILE
    }
    return Entry(nextEntry.name, type)
  }

  override fun closeNextEntryStream(entryStream: InputStream) {
  }

  override fun nextEntryStream(): InputStream? = stream

  override fun closeStream() {
    stream.close()
  }
}

internal class TarDecompressor(private val tarFile: Path, sizeLimit: Long?) : Decompressor(sizeLimit) {
  private var stream: TarArchiveInputStream? = null

  override fun openStream() {
    stream = try {
      val compressorStream = CompressorStreamFactory().createCompressorInputStream(tarFile.inputStream())
      TarArchiveInputStream(compressorStream)
    } catch (e: CompressorException) {
      val cause = e.cause
      if (cause is IOException) {
        throw cause
      }
      throw e
    }
  }

  override fun nextEntry(): Entry? {
    while (true) {
      val nextTarEntry = stream!!.nextTarEntry ?: return null
      val type = when {
        nextTarEntry.isFile -> Type.FILE
        nextTarEntry.isDirectory -> Type.DIR
        nextTarEntry.isSymbolicLink -> Type.SYMLINK
        else -> null
      } ?: continue
      return Entry(nextTarEntry.name, type)
    }
  }

  override fun nextEntryStream(): InputStream? = stream

  override fun closeNextEntryStream(entryStream: InputStream) = Unit //Do not close Tar entry stream.

  override fun closeStream() {
    stream?.close()
    stream = null
  }
}
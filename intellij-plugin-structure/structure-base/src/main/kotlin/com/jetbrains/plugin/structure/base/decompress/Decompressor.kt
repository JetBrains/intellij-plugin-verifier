/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.decompress

import com.jetbrains.plugin.structure.base.utils.bufferedInputStream
import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
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

  abstract val archivePath: Path
  abstract val archiveType: String

  @Throws(DecompressorException::class)
  fun extract(outputDir: Path) {
    val createdDirs = HashSet<Path>()
    val event = DecompressionEvent(archivePath.toString(), archiveType)
    event.begin()
    openStream()
    try {
      val actualSizeLimit = outputSizeLimit ?: Long.MAX_VALUE
      var remainingSize = actualSizeLimit
      loop@ while (true) {
        val entry = nextEntry() ?: break
        val outputFile = getEntryFile(outputDir, entry)
        when (entry.type) {
          Type.DIR -> {
            if (createdDirs.add(outputFile)) {
              outputFile.createDir()
              recordAncestors(outputFile, outputDir, createdDirs)
            }
          }
          Type.FILE -> {
            val entryStream = nextEntryStream() ?: continue@loop
            try {
              val countingStream = BoundedInputStream.builder()
                .setInputStream(entryStream)
                .setMaxCount(remainingSize + 1)
                .setPropagateClose(false)
                .get()

              val parent = outputFile.parent
              if (parent != null && createdDirs.add(parent)) {
                parent.createDir()
                recordAncestors(parent, outputDir, createdDirs)
              }
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
      event.commit()
    }
  }

  private fun recordAncestors(dir: Path, stopAt: Path, createdDirs: MutableSet<Path>) {
    var p = dir.parent
    while (p != null && p != stopAt) {
      if (!createdDirs.add(p)) break  // already recorded — all further ancestors are too
      p = p.parent
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

@Throws(DecompressorException::class)
private fun getEntryFile(outputDir: Path, entry: Decompressor.Entry): Path {
  val independentEntryName = entry.name.toSystemIndependentName()
  val pathElements = independentEntryName.split("/").filter { it.isNotEmpty() }
  val normalizedEntryName = normalizePathTraversal(entry, pathElements)
  if (pathElements.any { it.length > Decompressor.FILE_NAME_LENGTH_LIMIT }) {
    throw EntryNameTooLongException.ofEntry(entry.name)
  }
  return outputDir.resolve(normalizedEntryName)
}

@Throws(DecompressorException::class)
private fun normalizePathTraversal(entry: Decompressor.Entry, pathElements: List<String>): String {
  val normalized = mutableListOf<String>()
  for (element in pathElements) {
    when (element) {
      "." -> continue
      ".." -> {
        if (normalized.isEmpty()) {
          throw InvalidRelativeEntryNameException.ofEntry(entry.name)
        }
        normalized.removeAt(normalized.lastIndex)
      }
      else -> {
        normalized += element
      }
    }
  }
  if (normalized.isEmpty()) {
    throw EmptyEntryNameException.ofEntry(entry.name)
  }
  return normalized.joinToString("/")
}

internal class ZipDecompressor(private val zipFile: Path, sizeLimit: Long?) : Decompressor(sizeLimit) {
  override val archivePath: Path get() = zipFile
  override val archiveType: String get() = "zip"

  private lateinit var stream: ZipInputStream

  override fun openStream() {
    stream = ZipInputStream(zipFile.bufferedInputStream())
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
  override val archivePath: Path get() = tarFile
  override val archiveType: String get() = "tar"

  private var stream: TarArchiveInputStream? = null

  override fun openStream() {
    stream = try {
      val compressorStream = CompressorStreamFactory().createCompressorInputStream(tarFile.bufferedInputStream())
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
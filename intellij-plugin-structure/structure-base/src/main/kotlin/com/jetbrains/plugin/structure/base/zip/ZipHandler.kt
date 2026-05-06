/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.zip.ZipEntry

interface ZipHandler<Z: ZipResource> {
  /**
   * Invokes the [handler] for each entry in the ZIP file.
   * All results of the [handler] invocations are collected and returned.
   * @param handler a handler to be invoked for each ZIP entry
   * @return a list of results produced by the [handler] function,
   * @throws ZipArchiveException when a ZIP archive is malformed or an I/O error occurred while reading it
   */
  @Throws(ZipArchiveException::class)
  fun <T> iterate(handler: (ZipEntry, Z) -> T?): List<T>

  /**
   * Searches the ZIP for the entry with the corresponding filename.
   * If such an entry is found, the handler is invoked.
   *
   * @param entryName the name of the ZIP entry, usually a filename, to find in the ZIP
   * @param handler a handler to invoke on the encountered entry
   * @throws ZipArchiveException when a ZIP archive is malformed or an I/O error occurred while reading it
   */
  @Throws(ZipArchiveException::class)
  fun <T> handleEntry(entryName: CharSequence, handler: (ZipEntry, Z) -> T?): T?

  /**
   * Indicates if the ZIP file contains an entry with the corresponding filename.
   *
   * @param entryName the name of the ZIP entry, usually a filename, to find in the ZIP
   * @throws ZipArchiveException when a ZIP archive is malformed or an I/O error occurred while reading it
   */
  @Throws(ZipArchiveException::class)
  fun containsEntry(entryName: CharSequence): Boolean

  /**
   * Returns the set of file entry names that appear more than once in this archive.
   * Duplicate entries are a sign of a malformed or potentially malicious archive: different
   * ZIP parsers may resolve the same name to different content depending on whether they use
   * local file headers (sequential scanners) or the central directory (JVM ZipFile/JarFile).
   *
   * @throws ZipArchiveException when a ZIP archive is malformed or an I/O error occurred while reading it
   */
  @Throws(ZipArchiveException::class)
  fun findDuplicateEntries(): Set<String> {
    val seen = mutableSetOf<String>()
    val duplicates = mutableSetOf<String>()
    iterate { entry, _ ->
      if (!entry.isDirectory && !seen.add(entry.name)) {
        duplicates += entry.name
      }
      null
    }
    return duplicates
  }
}

fun Path.newZipHandler(): ZipHandler<out ZipResource> {
  return if (supportsFile()) {
    ZipFileHandler(this)
  } else {
    ZipInputStreamHandler(this)
  }
}

private fun Path.supportsFile() = fileSystem == FileSystems.getDefault()

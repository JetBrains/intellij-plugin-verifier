/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipFileHandler(private val zipFile: File) : ZipHandler<ZipResource.ZipFileResource> {
  constructor(zipPath: Path) : this(zipPath.toFile())

  override fun <T> iterate(handler: (ZipEntry, ZipResource.ZipFileResource) -> T?): List<T> {
    val results = mutableListOf<T>()
    ZipFile(zipFile).use { zip ->
      val entries = zip.entries()
      val zipResource = ZipResource.ZipFileResource(zip)
      while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        handler(entry, zipResource)?.let { results += it }
      }
    }
    return results
  }

  override fun <T> handleEntry(entryName: CharSequence, handler: (ZipEntry, ZipResource.ZipFileResource) -> T?): T? {
    return ZipFile(zipFile).use { zip ->
      val zipResource = ZipResource.ZipFileResource(zip)
      val entry: ZipEntry? = zip.getEntry(entryName.toString())
      entry?.let  {
        handler(entry, zipResource)
      }
    }
  }
}
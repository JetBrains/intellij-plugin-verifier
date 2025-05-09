/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipInputStreamHandler(private val zipPath: Path) : ZipHandler<ZipResource.ZipStreamResource> {
  override fun <T> iterate(handler: (ZipEntry, ZipResource.ZipStreamResource) -> T?): List<T> {
    val results = mutableListOf<T>()
    Files.newInputStream(zipPath).use {
      ZipInputStream(it).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
          val resource = ZipResource.ZipStreamResource(zip)
          handler(entry, resource)?.let { results += it }

          zip.closeEntry()
          entry = zip.nextEntry
        }
      }
    }
    return results
  }

  override fun <T> handleEntry(entryName: CharSequence, handler: (ZipEntry, ZipResource.ZipStreamResource) -> T?): T? {
    return Files.newInputStream(zipPath).use {
      ZipInputStream(it).use { zip ->
        var entry: ZipEntry? = zip.nextEntry
        while (entry != null) {
          val resource = ZipResource.ZipStreamResource(zip)
          try {
            if (entry.name.contentEquals(entryName)) {
              return handler(entry, resource)
            }
          } finally {
            zip.closeEntry()
          }
          entry = zip.nextEntry
        }
        null
      }
    }
  }
}
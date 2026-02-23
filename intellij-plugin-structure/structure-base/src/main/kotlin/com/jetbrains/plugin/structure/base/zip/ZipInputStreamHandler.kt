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
    val event = ZipIterateEvent(zipPath.toString())
    event.begin()
    return try {
      zipPath.useZipInputStream { zipInputStream ->
        zipInputStream
          .asSequence()
          .mapNotNull { (zipEntry, zipInputStream) ->
            val resource = ZipResource.ZipStreamResource(zipInputStream)
            handler(zipEntry, resource)
          }
          .toList()
      }
    } finally {
      event.commit()
    }
  }

  override fun <T> handleEntry(entryName: CharSequence, handler: (ZipEntry, ZipResource.ZipStreamResource) -> T?): T? {
    val event = ZipHandleEntryEvent(zipPath.toString(), entryName.toString())
    event.begin()
    return try {
      zipPath.useZipInputStream { zipInputStream ->
        zipInputStream
          .asSequence()
          .firstOrNull { (zipEntry, _) ->
            zipEntry.name.contentEquals(entryName)
          }?.let { (zipEntry, zipInputStream) ->
            val resource = ZipResource.ZipStreamResource(zipInputStream)
            return handler(zipEntry, resource)
          }
      }
    } finally {
      event.commit()
    }
  }

  override fun containsEntry(entryName: CharSequence): Boolean {
    return zipPath.useZipInputStream { zipInputStream ->
      zipInputStream
        .asSequence()
        .any { (zipEntry, _) -> zipEntry.name.contentEquals(entryName) }
    }
  }

  private fun ZipInputStream.asSequence() = sequence<Pair<ZipEntry, ZipInputStream>> {
    val zip = this@asSequence
    while (true) {
      try {
        val entry = zip.nextEntry ?: break
        yield(entry to zip)
      } finally {
        zip.closeEntry()
      }
    }
  }

  private inline fun <T> Path.useZipInputStream(block: (ZipInputStream) -> T): T {
    return Files.newInputStream(this).use {
      ZipInputStream(it).use(block)
    }
  }
}
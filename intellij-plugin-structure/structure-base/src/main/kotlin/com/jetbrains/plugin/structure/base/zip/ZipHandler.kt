/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.zip.ZipEntry

interface ZipHandler<Z: ZipResource> {
  fun <T> iterate(handler: (ZipEntry, Z) -> T?): List<T>

  fun <T> handleEntry(entryName: CharSequence, handler: (Z, ZipEntry) -> T?): T?
}

fun Path.newZipHandler(): ZipHandler<out ZipResource> {
  return if (supportsFile()) {
    ZipFileHandler(toFile())
  } else {
    ZipInputStreamHandler(this)
  }
}

private fun Path.supportsFile() = fileSystem == FileSystems.getDefault()

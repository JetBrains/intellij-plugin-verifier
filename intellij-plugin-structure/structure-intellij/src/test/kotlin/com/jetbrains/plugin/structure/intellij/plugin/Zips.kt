/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.utils.outputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun createZip(zipPath: Path, files: Map<String, String>): Path {
  return ZipOutputStream(zipPath.outputStream().buffered()).use { zipOut ->
    for ((entryName, content) in files) {
      val entry = ZipEntry(entryName)
      zipOut.putNextEntry(entry)

      val data = content.toByteArray(Charsets.UTF_8)
      zipOut.write(data, 0, data.size)
      zipOut.closeEntry()
    }
    zipPath
  }
}
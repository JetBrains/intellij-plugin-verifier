/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import com.jetbrains.plugin.structure.base.utils.outputStream
import com.jetbrains.plugin.structure.base.utils.readBytes
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

internal fun createZip(zipPath: Path, vararg entrySpec: ZipEntrySpec): Path {
  return ZipOutputStream(zipPath.outputStream().buffered()).use { zipOut ->
    for (spec in entrySpec) {
      val entry = ZipEntry(spec.name)
      zipOut.putNextEntry(entry)

      val data = spec.toByteArray()
      zipOut.write(data, 0, data.size)
      zipOut.closeEntry()
    }
    zipPath
  }
}

internal sealed class ZipEntrySpec {
  abstract val name: String

  abstract fun toByteArray(): ByteArray

  class Plain(override val name: String, val content: String) : ZipEntrySpec() {
    override fun toByteArray() = content.toByteArray(Charsets.UTF_8)
  }

  class File(override val name: String, val localPath: Path) : ZipEntrySpec() {
    override fun toByteArray(): ByteArray {
      return localPath.readBytes()
    }
  }
}
/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.zip

import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

sealed class ZipResource {
  abstract fun getInputStream(zipEntry: ZipEntry): InputStream

  data class ZipFileResource(val zipFile: ZipFile) : ZipResource() {
    override fun getInputStream(zipEntry: ZipEntry): InputStream {
      return zipFile.getInputStream(zipEntry)
    }
  }

  data class ZipStreamResource(val zipStream: ZipInputStream) : ZipResource() {
    override fun getInputStream(zipEntry: ZipEntry): InputStream {
      return zipStream
    }
  }
}
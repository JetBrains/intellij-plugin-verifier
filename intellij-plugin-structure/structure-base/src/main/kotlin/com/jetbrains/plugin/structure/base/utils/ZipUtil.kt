/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.decompress.TarDecompressor
import com.jetbrains.plugin.structure.base.decompress.ZipCompressor
import com.jetbrains.plugin.structure.base.decompress.ZipDecompressor
import java.io.File

fun File.extractTo(destination: File, outputSizeLimit: Long? = null): File {
  val decompressor = when {
    name.endsWith(".zip") -> ZipDecompressor(this.inputStream(), outputSizeLimit)
    name.endsWith(".tar.gz") -> TarDecompressor(this.inputStream(), outputSizeLimit)
    else -> throw IllegalArgumentException("Unknown type archive type: ${destination.name}")
  }

  destination.createDir()
  decompressor.extract(destination)
  return destination
}

fun File.archiveDirectoryTo(destination: File) {
  require(destination.extension == "zip" || destination.extension == "jar" || destination.extension == "nupkg")
  destination.forceDeleteIfExists()
  ZipCompressor(destination).use { zip ->
    zip.addDirectory(this)
  }
}
/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.decompress.TarDecompressor
import com.jetbrains.plugin.structure.base.decompress.ZipCompressor
import com.jetbrains.plugin.structure.base.decompress.ZipDecompressor
import java.nio.file.Path

fun extractZip(pluginFile: Path, destination: Path, outputSizeLimit: Long? = null): Path {
  destination.createDir()
  ZipDecompressor(pluginFile, outputSizeLimit).extract(destination)
  return destination
}

fun Path.extractTo(destination: Path, outputSizeLimit: Long? = null): Path {
  val decompressor = when {
    simpleName.endsWith(".zip") || simpleName.endsWith(".sit") -> ZipDecompressor(this, outputSizeLimit)
    simpleName.endsWith(".tar.gz") -> TarDecompressor(this, outputSizeLimit)
    else -> throw IllegalArgumentException("Unknown type archive type: ${destination.fileName}")
  }

  destination.createDir()
  decompressor.extract(destination)
  return destination
}

fun Path.archiveDirectoryTo(destination: Path) {
  require(destination.extension == "zip" || destination.extension == "jar" || destination.extension == "nupkg")
  destination.forceDeleteIfExists()
  ZipCompressor(destination).use { zip ->
    zip.addDirectory(this)
  }
}
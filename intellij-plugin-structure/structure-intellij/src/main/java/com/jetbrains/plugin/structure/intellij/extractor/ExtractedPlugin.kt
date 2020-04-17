/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.extractor

import org.apache.commons.io.FileUtils
import java.io.Closeable
import java.io.File

data class ExtractedPlugin(
  val pluginFile: File,
  private val fileToDelete: File
) : Closeable {
  override fun close() {
    FileUtils.deleteQuietly(fileToDelete)
  }
}
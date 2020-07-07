/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.extractor

import com.jetbrains.plugin.structure.base.utils.deleteQuietly
import java.io.Closeable
import java.nio.file.Path

data class ExtractedPlugin(
  val pluginFile: Path,
  private val fileToDelete: Path
) : Closeable {
  override fun close() {
    fileToDelete.deleteQuietly()
  }
}
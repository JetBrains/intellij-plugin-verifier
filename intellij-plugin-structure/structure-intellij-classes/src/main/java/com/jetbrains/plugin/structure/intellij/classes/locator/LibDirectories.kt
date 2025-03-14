/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.intellij.classes.locator.LibDirectoryLocator.LibDirectoryFilter
import java.nio.file.Path

internal const val MODULES_DIR = "modules"
internal const val LIB_DIR = "lib"

object NotAModulesDirectoryFilter : LibDirectoryFilter {
  override fun accept(path: Path): Boolean {
    return path.fileName.toString() != MODULES_DIR
  }
}
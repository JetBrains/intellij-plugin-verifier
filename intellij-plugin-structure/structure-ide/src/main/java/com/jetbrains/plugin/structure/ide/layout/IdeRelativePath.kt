/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.base.utils.exists
import java.nio.file.Path

data class IdeRelativePath(val idePath: Path, val relativePath: Path) {
  val resolvedPath: Path? = idePath.resolve(relativePath)

  val exists: Boolean
    get() = resolvedPath?.exists() == true

  fun toList(): List<Path> = if (resolvedPath != null) listOf(resolvedPath) else emptyList()
}

/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.jps

import com.jetbrains.plugin.structure.base.utils.isDirectory
import java.nio.file.Path

internal fun isCompiledUltimate(ideaDir: Path) = getCompiledClassesRoot(ideaDir) != null &&
  ideaDir.resolve(".idea").isDirectory &&
  ideaDir.resolve("community").resolve(".idea").isDirectory

internal fun isCompiledCommunity(ideaDir: Path) = getCompiledClassesRoot(ideaDir) != null &&
  ideaDir.resolve(".idea").isDirectory &&
  !ideaDir.resolve("community").resolve(".idea").isDirectory

internal fun getCompiledClassesRoot(ideaDir: Path): Path? =
  listOf(
    ideaDir.resolve("out").resolve("production"),
    ideaDir.resolve("out").resolve("classes").resolve("production"),
    ideaDir.resolve("out").resolve("compilation").resolve("classes").resolve("production")
  ).find { it.isDirectory }

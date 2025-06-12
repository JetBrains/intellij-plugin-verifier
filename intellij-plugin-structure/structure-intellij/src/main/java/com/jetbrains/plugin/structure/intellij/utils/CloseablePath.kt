/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.utils

import com.jetbrains.plugin.structure.base.utils.deleteLogged
import java.nio.file.Path

class CloseablePath(val path: Path) : AutoCloseable {
  override fun close() {
    path.deleteLogged()
  }

  override fun toString() = "$path (auto-closeable)"
}
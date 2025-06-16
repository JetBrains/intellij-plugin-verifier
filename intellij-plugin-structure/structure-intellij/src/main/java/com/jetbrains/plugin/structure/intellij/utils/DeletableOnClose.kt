/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.utils

import com.jetbrains.plugin.structure.base.utils.Deletable
import java.io.Closeable

class DeletableOnClose(private val deletable: Deletable) : Closeable, Deletable {
  override fun close() = deletable.delete()

  override fun delete() = deletable.delete()

  companion object {
    fun of(closeable: Closeable): Closeable {
      return if (closeable is Deletable) {
        closeable as? DeletableOnClose ?: DeletableOnClose(closeable)
      } else {
        closeable
      }
    }
  }
}
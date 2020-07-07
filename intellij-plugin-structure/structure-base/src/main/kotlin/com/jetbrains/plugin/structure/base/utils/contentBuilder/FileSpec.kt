/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.contentBuilder

import com.jetbrains.plugin.structure.base.utils.createDir
import com.jetbrains.plugin.structure.base.utils.writeBytes
import java.nio.file.Path

class FileSpec(private val content: ByteArray) : ContentSpec {
  override fun generate(target: Path) {
    target.parent?.createDir()
    target.writeBytes(content)
  }
}
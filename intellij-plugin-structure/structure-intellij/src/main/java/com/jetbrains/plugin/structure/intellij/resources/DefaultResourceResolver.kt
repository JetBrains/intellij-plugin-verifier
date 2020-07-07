/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import java.nio.file.Path

object DefaultResourceResolver : ResourceResolver {
  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val path = basePath.resolveSibling(relativePath)
    if (path.exists()) {
      return try {
        ResourceResolver.Result.Found(path, path.inputStream())
      } catch (e: Exception) {
        ResourceResolver.Result.Failed(path, e)
      }
    }
    return ResourceResolver.Result.NotFound
  }
}

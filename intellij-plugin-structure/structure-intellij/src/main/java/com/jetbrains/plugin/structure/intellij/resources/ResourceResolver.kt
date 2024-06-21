/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.description
import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path

interface ResourceResolver {

  fun resolveResource(relativePath: String, basePath: Path): Result

  sealed class Result {
    data class Found(
      val path: Path,
      val resourceStream: InputStream,
      private val resourceToClose: Closeable = Closeable {  },
      val description: String = path.description
    ) : Result(), Closeable {
      override fun close() {
        resourceStream.close()
        resourceToClose.close()
      }
    }

    object NotFound : Result()

    data class Failed(val path: Path, val exception: Exception) : Result()
  }

}

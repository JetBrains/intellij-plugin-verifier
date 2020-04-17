/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.resources

import java.io.Closeable
import java.io.InputStream
import java.net.URL

interface ResourceResolver {

  fun resolveResource(relativePath: String, base: URL): Result

  sealed class Result {
    data class Found(val url: URL, val resourceStream: InputStream) : Result(), Closeable {
      override fun close() {
        resourceStream.close()
      }
    }

    object NotFound : Result()

    data class Failed(val url: URL, val exception: Exception) : Result()
  }

}

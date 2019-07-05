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

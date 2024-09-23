package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.isFile
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver.Result.*
import java.nio.file.Path

object SimpleResourceResolver : ResourceResolver {
  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val path = basePath.resolvePath(relativePath) ?: return NotFound
    return try {
      if (!path.exists()) return NotFound
      Found(path, path.inputStream())
    } catch (e: Exception) {
      Failed(path, e)
    }
  }

  private fun Path.resolvePath(relativePath: String): Path? {
    val p: Path? = if (isFile) parent else this
    return p?.resolve(relativePath)
  }
}
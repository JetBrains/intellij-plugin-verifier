package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver.Result.Found
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.io.Closeable
import java.nio.file.Path

class JarsResourceResolver(private val jarFiles: List<Path>, private val jarFileSystemProvider: JarFileSystemProvider) : ResourceResolver {
  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    val finalPath = basePath.resolveSibling(relativePath).toString()
    for (jarFile in jarFiles) {
      val result = resolveResource(jarFile, finalPath)
      if (result != null) {
        return result
      }
    }
    return ResourceResolver.Result.NotFound
  }

  @Throws(Exception::class)
  private fun resolveResource(jarPath: Path, pathWithinJar: String): ResourceResolver.Result? {
    try {
      val jarFs = jarFileSystemProvider.getFileSystem(jarPath)
      val path = jarFs.getPath(pathWithinJar)
      if (!path.exists()) {
        return null
      }
      val resourceToClose = CloseablePath(path, jarFileSystemProvider)
      return Found(path, path.inputStream(), resourceToClose)
    } catch (e: Throwable) {
      jarFileSystemProvider.close(jarPath)
      throw e
    }
  }

  private data class CloseablePath(private val path: Path, private val fileSystemProvider: JarFileSystemProvider) : Closeable {
    override fun close() {
      fileSystemProvider.close(path)
    }
  }
}
package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver.Result.Found
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
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
      val path: Path = jarFs.getPath(pathWithinJar)
      if (!path.exists()) {
        return null
      }
      return Found(path, path.inputStream(), resourceToClose = jarFs)
    } catch (e: Throwable) {
      throw e
    } finally {
      jarFileSystemProvider.close(jarPath)
    }
  }
}
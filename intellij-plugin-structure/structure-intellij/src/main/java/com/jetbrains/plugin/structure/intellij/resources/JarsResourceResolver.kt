package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver.Result.Found
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class JarsResourceResolver(private val jarFiles: List<Path>, private val jarFileSystemProvider: JarFileSystemProvider) : ResourceResolver {
  private val resourceOwnersByPath = ConcurrentHashMap<String, Path>()
  private val missingResources = ConcurrentHashMap.newKeySet<String>()

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    val finalPath = basePath.resolveSibling(relativePath).toString()
    if (missingResources.contains(finalPath)) {
      return ResourceResolver.Result.NotFound
    }
    resourceOwnersByPath[finalPath]?.let { cachedJarPath ->
      resolveResource(cachedJarPath, finalPath)?.let {
        return it
      }
      resourceOwnersByPath.remove(finalPath, cachedJarPath)
    }
    for (jarFile in jarFiles) {
      val result = resolveResource(jarFile, finalPath)
      if (result != null) {
        resourceOwnersByPath[finalPath] = jarFile
        return result
      }
    }
    missingResources += finalPath
    return ResourceResolver.Result.NotFound
  }

  @Throws(Exception::class)
  private fun resolveResource(jarPath: Path, pathWithinJar: String): ResourceResolver.Result? {
    var jarFs: FileSystem? = null
    try {
      jarFs = jarFileSystemProvider.getFileSystem(jarPath)
      val path = jarFs.getPath(pathWithinJar)
      if (!path.exists()) {
        return null
      }
      return Found(path, path.inputStream(), jarFs)
    } catch (e: Throwable) {
      jarFs?.close()
      throw e
    }
  }
}

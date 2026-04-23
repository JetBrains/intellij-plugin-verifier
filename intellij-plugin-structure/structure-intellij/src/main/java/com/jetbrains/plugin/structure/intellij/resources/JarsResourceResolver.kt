package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.description
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class JarsResourceResolver(private val jarFiles: List<Path>, private val jarFileSystemProvider: JarFileSystemProvider) : ResourceResolver {
  private val cachedJarEntries = ConcurrentHashMap<String, CachedJarEntry>()

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    val finalPath = basePath.resolveSibling(relativePath).toString()
    return when (val cachedJarEntry = cachedJarEntries[finalPath]) {
      is CachedJarEntry.Found -> cachedJarEntry.open(jarFileSystemProvider)
      CachedJarEntry.NotFound -> ResourceResolver.Result.NotFound
      null -> resolveAndCacheResource(finalPath)
    }
  }

  @Throws(Exception::class)
  private fun resolveAndCacheResource(pathWithinJar: String): ResourceResolver.Result {
    for (jarFile in jarFiles) {
      val result = resolveResource(jarFile, pathWithinJar)
      if (result != null) {
        cachedJarEntries.putIfAbsent(pathWithinJar, CachedJarEntry.Found(jarFile, pathWithinJar, jarFile.description))
        return result
      }
    }
    cachedJarEntries.putIfAbsent(pathWithinJar, CachedJarEntry.NotFound)
    return ResourceResolver.Result.NotFound
  }

  @Throws(Exception::class)
  private fun resolveResource(jarPath: Path, pathWithinJar: String): ResourceResolver.Result? {
    var jarFs: FileSystem? = null
    try {
      jarFs = jarFileSystemProvider.getFileSystem(jarPath)
      val path = jarFs.getPath(pathWithinJar)
      return if (path.exists()) {
        val foundJarFs = requireNotNull(jarFs)
        jarFs = null
        ResourceResolver.Result.Found(path, path.inputStream(), foundJarFs, jarPath.description)
      } else {
        jarFs.close()
        jarFs = null
        null
      }
    } finally {
      jarFs?.close()
    }
  }

  private sealed class CachedJarEntry {
    data class Found(
      val jarPath: Path,
      val pathWithinJar: String,
      val description: String
    ) : CachedJarEntry() {
      fun open(jarFileSystemProvider: JarFileSystemProvider): ResourceResolver.Result {
        var jarFs: FileSystem? = null
        try {
          jarFs = jarFileSystemProvider.getFileSystem(jarPath)
          val path = jarFs.getPath(pathWithinJar)
          return ResourceResolver.Result.Found(path, path.inputStream(), jarFs, description)
        } catch (e: Throwable) {
          jarFs?.close()
          throw e
        }
      }
    }

    object NotFound : CachedJarEntry()
  }
}

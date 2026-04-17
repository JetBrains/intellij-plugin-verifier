package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.withZipFsSeparator
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver.Result.Found
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class JarsResourceResolver(private val jarFiles: List<Path>, private val jarFileSystemProvider: JarFileSystemProvider) : ResourceResolver {
  private val lookupCache = ConcurrentHashMap<String, LookupResult>()

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    val finalPath = basePath.resolveSibling(relativePath).toString().withZipFsSeparator()
    return when (val lookupResult = lookupCache.computeIfAbsent(finalPath, ::resolveLookup)) {
      is LookupResult.Found -> openFoundResource(lookupResult)
      LookupResult.NotFound -> ResourceResolver.Result.NotFound
    }
  }

  @Throws(Exception::class)
  private fun resolveLookup(pathWithinJar: String): LookupResult {
    for (jarFile in jarFiles) {
      var jarFs: FileSystem? = null
      try {
        jarFs = jarFileSystemProvider.getFileSystem(jarFile)
        val path = jarFs.getPath(pathWithinJar)
        if (path.exists()) {
          return LookupResult.Found(jarFile, path.toString().withZipFsSeparator())
        }
      } catch (e: Throwable) {
        jarFs?.close()
        throw e
      } finally {
        jarFs?.close()
      }
    }
    return LookupResult.NotFound
  }

  private fun openFoundResource(lookupResult: LookupResult.Found): ResourceResolver.Result {
    var jarFs: FileSystem? = null
    try {
      jarFs = jarFileSystemProvider.getFileSystem(lookupResult.jarPath)
      val path = jarFs.getPath(lookupResult.pathWithinJar)
      return Found(path, path.inputStream(), jarFs)
    } catch (e: Throwable) {
      jarFs?.close()
      throw e
    }
  }

  private sealed class LookupResult {
    class Found(val jarPath: Path, val pathWithinJar: String) : LookupResult()
    object NotFound : LookupResult()
  }
}

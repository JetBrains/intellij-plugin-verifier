package com.jetbrains.plugin.structure.intellij.resources

import com.jetbrains.plugin.structure.base.utils.description
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.withZipFsSeparator
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class JarsResourceResolver(private val jarFiles: List<Path>, private val jarFileSystemProvider: JarFileSystemProvider) : ResourceResolver {
  private val cachedJarPaths = ConcurrentHashMap<String, Path>()

  override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
    val resourceResult = DefaultResourceResolver.resolveResource(relativePath, basePath)
    if (resourceResult !is ResourceResolver.Result.NotFound) {
      return resourceResult
    }
    val pathWithinJar = basePath.resolveSibling(relativePath).toString()
    cachedJarPaths[pathWithinJar]?.let { jarPath ->
      resolveResource(jarPath, pathWithinJar)?.let {
        return it
      }
      cachedJarPaths.remove(pathWithinJar, jarPath)
    }
    for (jarFile in jarFiles) {
      val result = resolveResource(jarFile, pathWithinJar)
      if (result != null) {
        cachedJarPaths.putIfAbsent(pathWithinJar, jarFile)
        return result
      }
    }
    return ResourceResolver.Result.NotFound
  }

  @Throws(Exception::class)
  private fun resolveResource(jarPath: Path, pathWithinJar: String) =
    jarFileSystemProvider.getFileSystem(jarPath).use { jarFs ->
      val path = jarFs.getPath(pathWithinJar.withZipFsSeparator()).takeIf { it.exists() } ?: return@use null
      val detachedPath = Path.of(pathWithinJar)
      val bytes = path.inputStream().use { it.readBytes() }
      ResourceResolver.Result.Found(detachedPath, ByteArrayInputStream(bytes), description = jarPath.description)
    }
}

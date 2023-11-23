package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.withSuperScheme
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * File system provider that maintains open file systems in an internal cache.
 */
class CachingJarFileSystemProvider : JarFileSystemProvider, AutoCloseable {
  private val fsCache: MutableMap<URI, FileSystem> = ConcurrentHashMap()

  private val delegateJarFileSystemProvider = UriJarFileSystemProvider { it.toUri().withSuperScheme(JAR_SCHEME) }

  override fun getFileSystem(jarPath: Path): FileSystem {
    val jarUri = jarPath.toJarFileUri()
    try {
      return fsCache.computeIfAbsent(jarUri) { delegateJarFileSystemProvider.getFileSystem(jarPath) }
    } catch (e: Throwable) {
      throw JarArchiveCannotBeOpenException(jarPath, jarUri, e)
    }
  }

  override fun close(jarPath: Path) {
    val jarUri = jarPath.toJarFileUri()
    fsCache[jarUri]?.let { fs ->
      fs.close()
      fsCache.remove(jarUri)
    }
  }

  override fun close() {
    fsCache.clear()
  }
}
package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.withSuperScheme
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * File system provider that mainains open file systems in an internal cache.
 */
class CachingJarFileSystemProvider : JarFileSystemProvider, AutoCloseable {
  private val fsCache: MutableMap<URI, FileSystem> = ConcurrentHashMap()

  override fun getFileSystem(jarPath: Path): FileSystem {
    try {
      val jarUri = jarPath.toJarFileUri()
      return fsCache.computeIfAbsent(jarUri) { getOrCreateFileSystem(jarPath) }
    } catch (e: Throwable) {
      throw JarArchiveCannotBeOpenException(jarPath, e)
    }
  }

  private fun getOrCreateFileSystem(jarPath: Path): FileSystem {
    val standardFileBasedJarFsProvider = UriJarFileSystemProvider { it.toJarFileUri() }
    val jarSchemaPrefixingFsProvider = UriJarFileSystemProvider { it.toUri().withSuperScheme(JAR_SCHEME) }
    for (provider in listOf(standardFileBasedJarFsProvider, jarSchemaPrefixingFsProvider)) {
      val fs = try {
        provider.getFileSystem(jarPath)
      } catch (e: Throwable) {
        null
      }
      if (fs != null) {
        return fs
      }
    }
    throw JarArchiveException("Filesystem cannot be retrieved for <$jarPath>. " +
      "No JAR filesystem provider was able to resolve the [" + jarPath + "]")
  }


  override fun close() {
    fsCache.clear()
  }
}
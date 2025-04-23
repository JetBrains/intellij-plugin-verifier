/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import com.github.benmanes.caffeine.cache.Caffeine
import com.jetbrains.plugin.structure.base.utils.withSuperScheme
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.TimeUnit


private val LOG: Logger = LoggerFactory.getLogger(CachingJarFileSystemProvider::class.java)

private const val MAX_OPEN_JAR_FILE_SYSTEMS: Long = 128

const val RETENTION_TIME_PROPERTY_NAME = "com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider.retentionTime"

class CachingJarFileSystemProvider(
  val retentionTimeInSeconds: Long = System.getProperty(RETENTION_TIME_PROPERTY_NAME)
    ?.toLongOrNull() ?: 10L
) : JarFileSystemProvider, AutoCloseable {

  private val delegateJarFileSystemProvider = UriJarFileSystemProvider { it.toUri().withSuperScheme(JAR_SCHEME) }

  private val fsCache = Caffeine.newBuilder()
    .maximumSize(MAX_OPEN_JAR_FILE_SYSTEMS)
    .expireAfterAccess(retentionTimeInSeconds, TimeUnit.SECONDS)
    .build<String, FsHandleFileSystem>()

  private fun createFsHandle(jarPath: Path, jarUri: URI): FsHandleFileSystem {
    val jarFs = delegateJarFileSystemProvider.getFileSystem(jarPath).also {
      LOG.debug("Creating a filesystem handler via delegate for <{}> (Cache size: {})", jarUri, fsCache.estimatedSize())
    }
    return FsHandleFileSystem(jarFs)
  }

  @Synchronized
  override fun getFileSystem(jarPath: Path): FileSystem {
    val jarUri = jarPath.toJarFileUri()
    val key = jarUri.toString()

    var fs = fsCache.getIfPresent(key)
    if (fs != null)  {
      if (fs.isOpen) {
        fs.increment()
        LOG.debug("Reusing filesystem handler for <{}> (Cache size: {})", key, fsCache.estimatedSize())
      } else {
        val jarFs = delegateJarFileSystemProvider.getFileSystem(jarPath).also {
          LOG.debug("Recreating an already closed a filesystem handler for <{}> (Cache size: {})", key, fsCache.estimatedSize())
        }
        fs = FsHandleFileSystem(jarFs)
        fsCache.put(key, fs)
      }
    } else {
      fs = createFsHandle(jarPath, jarUri)
      fsCache.put(key, fs)
    }
    return fs
  }

  override fun close() {
    fsCache.invalidateAll()
  }
}
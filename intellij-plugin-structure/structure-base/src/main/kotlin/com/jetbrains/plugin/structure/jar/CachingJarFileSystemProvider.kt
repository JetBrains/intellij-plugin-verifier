/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.withSuperScheme
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private const val MAX_OPEN_JAR_FILE_SYSTEMS = 256

private const val UNUSED_JAR_FILE_SYSTEMS_TO_CLOSE = 64

const val RETENTION_TIME_PROPERTY_NAME = "com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider.retentionTime"

private val LOG: Logger = LoggerFactory.getLogger(CachingJarFileSystemProvider::class.java)

class CachingJarFileSystemProvider(
  val retentionTimeInSeconds: Long = System.getProperty(RETENTION_TIME_PROPERTY_NAME)
    ?.toLongOrNull() ?: 10L
) : JarFileSystemProvider, AutoCloseable {

  private val fsCache = ConcurrentHashMap<URI, FSHandle>()

  private val clock = Clock.systemUTC()

  private val delegateJarFileSystemProvider = UriJarFileSystemProvider { it.toUri().withSuperScheme(JAR_SCHEME) }

  override fun getFileSystem(jarPath: Path): FileSystem {
    return getOrOpenFsHandler(jarPath).fs
  }

  @Synchronized
  private fun getOrOpenFsHandler(jarPath: Path): FSHandle {
    val jarUri = jarPath.toJarFileUri()

    val fsHandle = fsCache.getOrPut(jarUri) {
      val jarFs = delegateJarFileSystemProvider.getFileSystem(jarPath).also {
        LOG.debug("Opening a filesystem handler via delegate for <{}> (Cache size: {})", jarUri, fsCache.size)
      }
      FSHandle(jarFs, jarUri, clock.instant(), 0)
    }

    fsHandle.users++
    fsHandle.lastAccessTime = clock.instant()
    cleanup()
    return fsHandle
  }

  @Synchronized
  private fun cleanup() {
    if (fsCache.size <= MAX_OPEN_JAR_FILE_SYSTEMS) return

    val handlesToExpire = fsCache.values
      .filter { it.shouldExpire }
      .sortedBy { it.lastAccessTime }
      .take(UNUSED_JAR_FILE_SYSTEMS_TO_CLOSE)
      .also(::logHandles)

    handlesToExpire.forEach {
      expire(it)
    }
  }

  private fun expire(fsHandle: FSHandle) {
    fsHandle.close()
    fsCache.remove(fsHandle.uri)
    LOG.debug("Expiring filesystem handler for <{}>", fsHandle.uri)
  }

  @Synchronized
  override fun close(jarPath: Path) {
    val jarUri = jarPath.toJarFileUri()
    val fsHandle = fsCache[jarUri] ?: return
    with(fsHandle) {
      users--
      if (shouldExpire) {
        System.err.println("No useres and expired. Closing FS for $jarUri. Users: $users")
        expire(this)
      } else {
        System.err.println("Users: $users. Retention: $retentionTimeInSeconds.")
      }
    }
  }

  private val FSHandle.shouldExpire: Boolean
    get() = users == 0 && hasTimedOut

  private val FSHandle.hasTimedOut: Boolean
    get() {
      if (retentionTimeInSeconds == 0L) return true
      val now = clock.instant()
      return lastAccessTime.plusSeconds(retentionTimeInSeconds).isBefore(now)
    }

  private data class FSHandle(
    val fs: FileSystem,
    val uri: URI,
    var lastAccessTime: Instant,
    var users: Int = 0
  ): Closeable {
    override fun close() {
      try {
        fs.close()
      } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        LOG.info("Cannot close due to an interruption for [{}]", fs)
      } catch (_: NoSuchFileException) {
        LOG.debug("Cannot close as the file no longer exists for [{}]", fs)
      } catch (_: java.nio.file.NoSuchFileException) {
        LOG.debug("Cannot close as the file no longer exists for [{}]", fs)
      } catch (e: Exception) {
        LOG.error("Unable to close [{}]", fs, e)
      }
    }
  }

  @Synchronized
  override fun close() {
    fsCache.values.forEach { fsHandle ->
      expire(fsHandle)
    }
  }

  private fun logHandles(handles: Collection<FSHandle>) {
    if(handles.isEmpty()) return
    LOG.debug("Will expire {} cached FS entries", handles.size)
  }
}
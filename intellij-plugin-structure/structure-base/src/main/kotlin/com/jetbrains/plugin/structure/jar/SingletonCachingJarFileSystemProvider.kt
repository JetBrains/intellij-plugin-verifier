package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.closeLogged
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

/**
 * A singleton file system provider that maintains open file systems in an internal cache.
 *
 * This cache is limited in size. Entries expire by the LRU philosophy.
 *
 * @see [com.jetbrains.plugin.structure.classes.resolvers.JarFileSystemsPool]
 */
object SingletonCachingJarFileSystemProvider : JarFileSystemProvider, AutoCloseable {
  private val LOG: Logger = LoggerFactory.getLogger(javaClass)

  private const val MAX_OPEN_JAR_FILE_SYSTEMS = 256

  private const val UNUSED_JAR_FILE_SYSTEMS_TO_CLOSE = 64

  private val retentionTimeInSeconds =
    System.getenv("com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider.retentionTime")
      ?.toLongOrNull() ?: 10L

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
      .also { LOG.debug("Will expire {} cached FS entries", it.size) }

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
        expire(this)
      }
    }
  }

  private val FSHandle.shouldExpire: Boolean
    get() = users == 0 && hasTimedOut


  private val FSHandle.hasTimedOut: Boolean
    get() {
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
      fs.closeLogged()
    }
  }

  @Synchronized
  override fun close() {
    fsCache.values.forEach { fsHandle ->
      expire(fsHandle)
    }
  }
}
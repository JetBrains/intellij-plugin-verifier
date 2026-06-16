/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import com.jetbrains.plugin.structure.base.utils.withSuperScheme
import com.jetbrains.plugin.structure.jar.CachingJarFileSystemProvider.EventLog.Event.*
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider.Companion.DEFAULT_EXPECTED_CLIENTS
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger


private val LOG: Logger = LoggerFactory.getLogger(CachingJarFileSystemProvider::class.java)

private const val MAX_OPEN_JAR_FILE_SYSTEMS: Long = 256
private const val MAX_GET_FILE_SYSTEM_RETRIES = 1_024

const val RETENTION_TIME_PROPERTY_NAME = "com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider.retentionTime"

class CachingJarFileSystemProvider(
  val retentionTimeInSeconds: Long = System.getProperty(RETENTION_TIME_PROPERTY_NAME)
    ?.toLongOrNull() ?: 10L,
  private val enableEventLogging: Boolean = false
) : JarFileSystemProvider, AutoCloseable {

  private val delegateJarFileSystemProvider = UriJarFileSystemProvider { it.toUri().withSuperScheme(JAR_SCHEME) }

  private val delegateRefCounts = ConcurrentHashMap<DelegateFileSystemKey, AtomicInteger>()

  private val fsCache = Caffeine.newBuilder()
    .maximumSize(MAX_OPEN_JAR_FILE_SYSTEMS)
    .expireAfterAccess(retentionTimeInSeconds, TimeUnit.SECONDS)
    .removalListener(object : RemovalListener<String, FsHandleFileSystem> {
      override fun onRemoval(key: String?, value: FsHandleFileSystem?, cause: RemovalCause?) {
        value?.onCacheRemoval()
      }
    })
    .build<String, FsHandleFileSystem>()

  val eventLog = EventLog()

  private fun createFileSystem(jarPath: Path, jarUri: URI): FsHandleFileSystem {
    val key = jarUri.toString()
    val jarFs = delegateJarFileSystemProvider.getFileSystem(jarPath).also {
      LOG.debug("Creating a filesystem handler via delegate for <{}> (Cache size: {})", jarUri, fsCache.estimatedSize())
    }
    retainDelegate(jarFs)
    return fsHandleFileSystem(key, jarFs, jarPath)
  }

  private fun releaseDelegate(key: String, delegate: FileSystem) {
    synchronized(key.intern()) {
      releaseDelegate(delegate)
    }
  }

  private fun replaceDelegate(key: String, oldDelegate: FileSystem, newDelegate: FileSystem) {
    if (oldDelegate === newDelegate) {
      return
    }
    synchronized(key.intern()) {
      retainDelegate(newDelegate)
      releaseDelegate(oldDelegate)
    }
  }

  private fun retainDelegate(delegate: FileSystem) {
    delegateRefCounts.computeIfAbsent(DelegateFileSystemKey(delegate)) { AtomicInteger(0) }.incrementAndGet()
  }

  private fun releaseDelegate(delegate: FileSystem) {
    val delegateKey = DelegateFileSystemKey(delegate)
    val count = delegateRefCounts[delegateKey]
    if (count != null && count.decrementAndGet() == 0) {
      delegateRefCounts.remove(delegateKey)
      try {
        if (delegate.isOpen) delegate.close()
      } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
        LOG.info("Cannot close delegate due to an interruption for [{}]", delegate)
      } catch (e: Exception) {
        LOG.error("Unable to close delegate [{}]", delegate, e)
      }
    }
  }

  private fun fsHandleFileSystem(key: String, delegate: FileSystem, jarPath: Path): FsHandleFileSystem {
    return FsHandleFileSystem(
      delegate,
      delegateJarFileSystemProvider,
      jarPath,
      onDelegateRelease = { releasedDelegate -> releaseDelegate(key, releasedDelegate) },
      onDelegateReplace = { oldDelegate, newDelegate -> replaceDelegate(key, oldDelegate, newDelegate) }
    )
  }

  override fun getFileSystem(jarPath: Path): FileSystem {
    return getFileSystem(jarPath, expectedClients = DEFAULT_EXPECTED_CLIENTS)
  }

  override fun getFileSystem(jarPath: Path, configuration: Configuration): FileSystem {
    return getFileSystem(jarPath, configuration.expectedClients)
  }

  private fun getFileSystem(jarPath: Path, expectedClients: Int): FileSystem {
    val jarUri = jarPath.toJarFileUri()
    val key = jarUri.toString()

    // Retry because increment() must run outside the per-key lock: while it is running,
    // Caffeine may evict the cached handle or another thread may replace it. In that case
    // undo the acquired references, re-read the cache, and try again against the current entry.
    var retryAttempts = 0
    while (retryAttempts < MAX_GET_FILE_SYSTEM_RETRIES) {
      retryAttempts++

      val cachedFs = synchronized(key.intern()) {
        fsCache.getIfPresent(key)
      }

      if (cachedFs != null) {
        // increment() may reopen the delegate and call back into replaceDelegate(), which also
        // takes the per-key lock. Keep it outside that lock to avoid key -> fs / fs -> key inversion.
        if (cachedFs.increment(expectedClients)) {
          synchronized(key.intern()) {
            if (fsCache.getIfPresent(key) === cachedFs) {
              logReusedFs(key)
              return cachedFs
            }
          }
          repeat(expectedClients) {
            cachedFs.close()
          }
          continue
        }
      }

      val resolvedFs = synchronized(key.intern()) {
        val currentFs = fsCache.getIfPresent(key)
        if (currentFs !== cachedFs) {
          null
        } else if (currentFs != null) {
          val jarFs = delegateJarFileSystemProvider.getFileSystem(jarPath).also {
            LOG.debug("Recreating an already closed filesystem handler for <{}> (Cache size: {})", key, fsCache.estimatedSize())
          }
          retainDelegate(jarFs)
          val fs = fsHandleFileSystem(key, jarFs, jarPath)
          fsCache.put(key, fs)
          logRecreatedFs(key)
          fs
        } else {
          val fs = createFileSystem(jarPath, jarUri)
          fsCache.put(key, fs)
          logCreatedFs(key)
          fs
        }
      }
      if (resolvedFs != null) {
        return resolvedFs
      }
    }

    throw newRetryAttemptsExhausted(key.intern(), jarPath)
  }

  private fun newRetryAttemptsExhausted(jarUriKey: String, jarPath: Path): JarArchiveException {
    val cacheSize = fsCache.estimatedSize()
    val message = "Unable to resolve cached JAR filesystem for [$jarPath] (resolved URI: <$jarUriKey>) " +
      "after $MAX_GET_FILE_SYSTEM_RETRIES attempts (Cache size: $cacheSize). " +
      "Aborting to avoid a possible live lock."
    return JarArchiveException(message).also { ex ->
      LOG.error(
        message,
        ex
      )
    }
  }

  override fun close() {
    fsCache.asMap().values.forEach { it.onCacheRemoval() }
    fsCache.invalidateAll()
    fsCache.cleanUp()
  }

  private fun logCreatedFs(uriString: String) {
    if (enableEventLogging) eventLog.logCreated(uriString)
  }

  private fun logReusedFs(uriString: String) {
    LOG.debug("Reusing filesystem handler for <{}> (Cache size: {})", uriString, fsCache.estimatedSize())
    if (enableEventLogging) eventLog.logReused(uriString)
  }

  private fun logRecreatedFs(uriString: String) {
    if (enableEventLogging) eventLog.logRecreated(uriString)
  }

  class EventLog : AbstractList<EventLog.Event>() {
    private val events = mutableListOf<Event>()

    fun logCreated(key: String) {
      events += Created(key)
    }

    fun logReused(key: String) {
      events += Reused(key)
    }

    fun logRecreated(key: String) {
      events += Recreated(key)
    }

    override val size: Int
      get() = events.size

    override fun get(index: Int): Event = events[index]

    sealed class Event {
      data class Created(val key: String) : Event()
      data class Reused(val key: String) : Event()
      data class Recreated(val key: String) : Event()
    }
  }

  private class DelegateFileSystemKey(private val delegate: FileSystem) {
    override fun equals(other: Any?): Boolean {
      return other is DelegateFileSystemKey && delegate === other.delegate
    }

    override fun hashCode(): Int = System.identityHashCode(delegate)
  }
}

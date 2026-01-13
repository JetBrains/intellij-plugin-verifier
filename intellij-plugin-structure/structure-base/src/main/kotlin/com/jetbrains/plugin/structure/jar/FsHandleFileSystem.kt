/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.fs.FsHandlerFileSystemProvider
import com.jetbrains.plugin.structure.fs.FsHandlerPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.*
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.NoSuchFileException

private val LOG: Logger = LoggerFactory.getLogger(FsHandleFileSystem::class.java)

/**
 * Reference counting [FileSystem] wrapper that can reopen closed filesystem.
 *
 * When the reference counter reaches zero, the [FsHandleFileSystem.closeDelegate] is called.
 *
 * The instance is first initialized with an `initialDelegateFileSystem` as an underlying delegate
 * for Java NIO [FileSystem] operations.
 * However, when the underlying [FileSystem] is closed, the [FsHandleFileSystem] will
 * reopen the instance via `provider` and replace its delegate.
 *
 * @param initialDelegateFileSystem an initial low-level Java NIO [FileSystem] to wrap
 * @param provider a [JarFileSystemProvider] that created this filesystem.
 * @param path a [Path] to the JAR or ZIP managed by this filesystem.
 *
 * @see [FsHandlerFileSystemProvider]
 * @see [FsHandlerPath]
 */
class FsHandleFileSystem(
  val initialDelegateFileSystem: FileSystem,
  private val provider: JarFileSystemProvider,
  private val path: Path
) : FileSystem() {

  // '-1' for closed
  private val referenceCount = AtomicInteger(1)

  private var _delegateFileSystem = initialDelegateFileSystem
  val delegateFileSystem: FileSystem get() = getOrReopenDelegateFileSystem()

  /**
   * Returns true if FS is opened
   */
  fun increment(): Boolean {
    return increment(1)
  }

  /**
   * Returns true if FS is opened
   */
  fun increment(amount: Int): Boolean {
    while (true) {
      val current = referenceCount.get()
      if (current < 0) {
        return false
      }
      // might reopen FS
      if (!delegateFileSystem.isOpen) {
        return false
      }
      val updated = current + amount
      if (referenceCount.compareAndSet(current, updated)) {
        return true
      }
    }
  }

  private fun getOrReopenDelegateFileSystem(): FileSystem {
    var fs = synchronized(this) { _delegateFileSystem }
    if (fs.isOpen) {
      return fs
    }
    synchronized(this) {
      fs = _delegateFileSystem
      if (fs.isOpen) {
        return fs
      }
      LOG.debug("Reopening filesystem delegate for <{}>", path)
      fs = provider.getFileSystem(path)
      _delegateFileSystem = fs
      return fs
    }
  }

  override fun close() {
    while (true) {
      val current = referenceCount.get()
      if (current <= 0) {
        return
      }
      if (!referenceCount.compareAndSet(current, current - 1)) {
        continue
      }
      if (current == 1) {
        // was the last one, means referenceCount == 0, let's close and mark as closed
        closeDelegate()
        referenceCount.set(-1)
      }
      return
    }
  }

  fun closeDelegate() {
    val fs = synchronized(this) { _delegateFileSystem }
    try {
      if (fs.isOpen) fs.close()
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

  override fun isOpen(): Boolean = referenceCount.get() >= 0 && delegateFileSystem.isOpen

  override fun isReadOnly(): Boolean = delegateFileSystem.isReadOnly

  override fun getSeparator(): String = delegateFileSystem.separator

  override fun getRootDirectories(): Iterable<Path> = delegateFileSystem.rootDirectories.map { FsHandlerPath(this, it) }

  override fun getFileStores(): Iterable<FileStore> = delegateFileSystem.fileStores

  override fun supportedFileAttributeViews(): Set<String> = delegateFileSystem.supportedFileAttributeViews()

  override fun getPath(first: String, vararg more: String?): Path = FsHandlerPath(this, delegateFileSystem.getPath(first, *more))

  override fun getPathMatcher(syntaxAndPattern: String): PathMatcher = delegateFileSystem.getPathMatcher(syntaxAndPattern)

  override fun getUserPrincipalLookupService(): UserPrincipalLookupService = delegateFileSystem.userPrincipalLookupService

  override fun newWatchService(): WatchService = delegateFileSystem.newWatchService()

  override fun provider(): FileSystemProvider = FsHandlerFileSystemProvider(delegateFileSystem.provider(), provider)

  fun hasSameDelegate(fs: FileSystem): Boolean {
    return if (fs is FsHandleFileSystem) this.delegateFileSystem == fs.delegateFileSystem else false
  }
}
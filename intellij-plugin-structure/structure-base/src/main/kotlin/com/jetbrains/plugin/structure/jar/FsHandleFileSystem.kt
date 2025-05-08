/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.fs.isClosed
import com.jetbrains.plugin.structure.fs.FsHandlerFileSystemProvider
import com.jetbrains.plugin.structure.fs.FsHandlerPath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.ClosedFileSystemException
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.WatchService
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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

  private val isOpen = AtomicBoolean(true)

  private val referenceCount = AtomicInteger(1)

  private var _delegateFileSystem = initialDelegateFileSystem
  val delegateFileSystem: FileSystem get() = getOrReopenDelegateFileSystem()

  fun increment() {
    referenceCount.incrementAndGet()
  }

  fun increment(amount: Int) {
    referenceCount.addAndGet(amount)
  }

  @Synchronized
  private fun getOrReopenDelegateFileSystem(): FileSystem {
    if (_delegateFileSystem.isClosed) {
      LOG.debug("Reopening filesystem delegate for <{}>", path)
      _delegateFileSystem = provider.getFileSystem(path)
    }
    return _delegateFileSystem
  }

  @Synchronized
  override fun close() {
    if (!isOpen.get()) {
      throw ClosedFileSystemException()
    }

    if (referenceCount.decrementAndGet() == 0) {
      closeDelegate()
      isOpen.set(false)
    }
  }

  @Synchronized
  fun closeDelegate() {
    try {
      if (delegateFileSystem.isOpen) delegateFileSystem.close()
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
      LOG.info("Cannot close due to an interruption for [{}]", delegateFileSystem)
    } catch (_: NoSuchFileException) {
      LOG.debug("Cannot close as the file no longer exists for [{}]", delegateFileSystem)
    } catch (_: java.nio.file.NoSuchFileException) {
      LOG.debug("Cannot close as the file no longer exists for [{}]", delegateFileSystem)
    } catch (e: Exception) {
      LOG.error("Unable to close [{}]", delegateFileSystem, e)
    }
  }

  override fun isOpen(): Boolean = isOpen.get() && delegateFileSystem.isOpen

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
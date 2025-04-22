package com.jetbrains.plugin.structure.jar

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
import java.util.concurrent.atomic.AtomicInteger

private val LOG: Logger = LoggerFactory.getLogger(FsHandleFileSystem::class.java)
class FsHandleFileSystem(val delegate: FileSystem) : FileSystem() {

  private var isOpen = true

  private val referenceCount = AtomicInteger(1)

  fun increment() {
    referenceCount.incrementAndGet()
  }

  @Synchronized
  override fun close() {
    if (!isOpen) {
      throw ClosedFileSystemException()
    }

    if (referenceCount.decrementAndGet() == 0) {
      closeDelegate()
      isOpen = false
    }
  }

  fun closeDelegate() {
    try {
      if (delegate.isOpen) delegate.close()
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
      LOG.info("Cannot close due to an interruption for [{}]", delegate)
    } catch (_: NoSuchFileException) {
      LOG.debug("Cannot close as the file no longer exists for [{}]", delegate)
    } catch (_: java.nio.file.NoSuchFileException) {
      LOG.debug("Cannot close as the file no longer exists for [{}]", delegate)
    } catch (e: Exception) {
      LOG.error("Unable to close [{}]", delegate, e)
    }
  }

  override fun isOpen(): Boolean = isOpen && delegate.isOpen

  override fun isReadOnly(): Boolean = delegate.isReadOnly

  override fun getSeparator(): String = delegate.separator

  override fun getRootDirectories(): Iterable<Path> = delegate.rootDirectories

  override fun getFileStores(): Iterable<FileStore> = delegate.fileStores

  override fun supportedFileAttributeViews(): Set<String> = delegate.supportedFileAttributeViews()

  override fun getPath(first: String, vararg more: String?): Path = delegate.getPath(first, *more)

  override fun getPathMatcher(syntaxAndPattern: String): PathMatcher = delegate.getPathMatcher(syntaxAndPattern)

  override fun getUserPrincipalLookupService(): UserPrincipalLookupService = delegate.userPrincipalLookupService

  override fun newWatchService(): WatchService = delegate.newWatchService()

  override fun provider(): FileSystemProvider = delegate.provider()

  fun hasSameDelegate(fs: FileSystem): Boolean {
    return if (fs is FsHandleFileSystem) this.delegate == fs.delegate else false
  }
}
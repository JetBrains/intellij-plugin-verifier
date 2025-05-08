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

class FsHandleFileSystem(
  val initialDelegateFileSystem: FileSystem,
  private val provider: JarFileSystemProvider,
  private val path: Path? = null
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
    if (_delegateFileSystem.isClosed && path != null) {
      val reopenedFS = provider.getFileSystem(path)
      LOG.debug("Reopening filesystem delegate for <{}>", path)
      _delegateFileSystem = reopenedFS
      return _delegateFileSystem
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
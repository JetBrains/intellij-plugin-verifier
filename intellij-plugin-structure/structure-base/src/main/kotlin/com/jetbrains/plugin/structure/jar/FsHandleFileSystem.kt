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
import java.util.concurrent.atomic.AtomicInteger

private val LOG: Logger = LoggerFactory.getLogger(FsHandleFileSystem::class.java)
class FsHandleFileSystem(val initialDelegateFileSystem: FileSystem, private val path: Path? = null) : FileSystem() {

  private var isOpen = true

  private val referenceCount = AtomicInteger(1)

  private var _delegateFileSystem = initialDelegateFileSystem
  val delegateFileSystem: FileSystem
    get() {
      if (_delegateFileSystem.isClosed && path != null) {
        val reopenedFsWrapper = SingletonCachingJarFileSystemProvider.getFileSystem(path)
        if (reopenedFsWrapper is FsHandleFileSystem) {
          LOG.debug("Reopening filesystem delegate for <{}>", path)
          _delegateFileSystem = reopenedFsWrapper.initialDelegateFileSystem
          return _delegateFileSystem
        } else {
          LOG.debug("Filesystem delegate cannot be reopened for <{}>: unsupported type '{}'", path, reopenedFsWrapper.javaClass.simpleName)
        }
      }
      return _delegateFileSystem
    }

  fun increment() {
    referenceCount.incrementAndGet()
  }

  fun increment(amount: Int) {
    referenceCount.addAndGet(amount)
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

  override fun isOpen(): Boolean = isOpen && delegateFileSystem.isOpen

  override fun isReadOnly(): Boolean = delegateFileSystem.isReadOnly

  override fun getSeparator(): String = delegateFileSystem.separator

  override fun getRootDirectories(): Iterable<Path> = delegateFileSystem.rootDirectories.map { FsHandlerPath(this, it) }

  override fun getFileStores(): Iterable<FileStore> = delegateFileSystem.fileStores

  override fun supportedFileAttributeViews(): Set<String> = delegateFileSystem.supportedFileAttributeViews()

  override fun getPath(first: String, vararg more: String?): Path = FsHandlerPath(this, delegateFileSystem.getPath(first, *more))

  override fun getPathMatcher(syntaxAndPattern: String): PathMatcher = delegateFileSystem.getPathMatcher(syntaxAndPattern)

  override fun getUserPrincipalLookupService(): UserPrincipalLookupService = delegateFileSystem.userPrincipalLookupService

  override fun newWatchService(): WatchService = delegateFileSystem.newWatchService()

  override fun provider(): FileSystemProvider = FsHandlerFileSystemProvider(delegateFileSystem.provider())

  fun hasSameDelegate(fs: FileSystem): Boolean {
    return if (fs is FsHandleFileSystem) this.delegateFileSystem == fs.delegateFileSystem else false
  }
}
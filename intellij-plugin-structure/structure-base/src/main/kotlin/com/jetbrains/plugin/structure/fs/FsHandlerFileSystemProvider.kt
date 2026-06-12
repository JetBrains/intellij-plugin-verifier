/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fs

import com.jetbrains.plugin.structure.base.fs.isClosed
import com.jetbrains.plugin.structure.jar.FsHandleFileSystem
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.AccessMode
import java.nio.file.ClosedFileSystemException
import java.nio.file.CopyOption
import java.nio.file.DirectoryStream
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.spi.FileSystemProvider

private const val MAX_REOPEN_ATTEMPTS = 3

class FsHandlerFileSystemProvider(
  val delegateProvider: FileSystemProvider,
  private val delegateJarFileSystemProvider: JarFileSystemProvider
) : FileSystemProvider() {
  override fun getScheme(): String? = delegateProvider.scheme

  override fun newFileSystem(uri: URI, env: Map<String, *>): FileSystem =
    FsHandleFileSystem(
      delegateProvider.newFileSystem(uri, env),
      delegateJarFileSystemProvider,
      uri.toPath()
    )

  override fun getFileSystem(uri: URI): FsHandleFileSystem = FsHandleFileSystem(
    delegateProvider.getFileSystem(uri),
    delegateJarFileSystemProvider,
    uri.toPath()
  )

  override fun getPath(uri: URI): Path = FsHandlerPath(getFileSystem(uri), uri.toPath())

  override fun newByteChannel(
    path: Path,
    options: Set<OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel = retryOnClosedFs(path) { p ->
    delegateProvider.newByteChannel(p, options, *attrs)
  }

  override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>): DirectoryStream<Path> {
    val inner = delegateProvider.newDirectoryStream(dir.unwrapped, filter)
    // Only wrap entries when the caller actually came through the FsHandlerPath layer.
    // For a raw delegate path, dir.fileSystem is the underlying ZipFileSystem, which would
    // produce FsHandlerPath instances that bypass the reopen mechanism anyway.
    val fs = (dir as? FsHandlerPath)?.fileSystem ?: return inner
    return object : DirectoryStream<Path> {
      override fun iterator(): MutableIterator<Path> = object : MutableIterator<Path> {
        private val delegate = inner.iterator()
        override fun hasNext(): Boolean = delegate.hasNext()
        override fun next(): Path = FsHandlerPath(fs, delegate.next())
        override fun remove() = delegate.remove()
      }

      override fun close() = inner.close()
    }
  }

  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>?) {
    delegateProvider.createDirectory(dir.unwrapped, *attrs)
  }

  override fun delete(path: Path) {
    delegateProvider.delete(path.unwrapped)
  }

  override fun copy(source: Path, target: Path, vararg options: CopyOption) {
    delegateProvider.copy(source.unwrapped, target.unwrapped, *options)
  }

  override fun move(source: Path, target: Path, vararg options: CopyOption) {
    delegateProvider.move(source.unwrapped, target.unwrapped, *options)
  }

  override fun isSameFile(path: Path, path2: Path): Boolean {
    return if (path is FsHandlerPath && path2 is FsHandlerPath) {
      path.delegatePath.fileSystem.provider().isSameFile(path.delegatePath, path2.delegatePath)
    } else {
      delegateProvider.isSameFile(path.unwrapped, path2.unwrapped)
    }
  }

  override fun isHidden(path: Path) = delegateProvider.isHidden(path.unwrapped)

  override fun getFileStore(path: Path): FileStore = delegateProvider.getFileStore(path.unwrapped)

  override fun checkAccess(path: Path, vararg modes: AccessMode) = delegateProvider.checkAccess(path.unwrapped, *modes)

  override fun <V : FileAttributeView?> getFileAttributeView(
    path: Path,
    type: Class<V>,
    vararg options: LinkOption
  ): V? = delegateProvider.getFileAttributeView(path.unwrapped, type, *options)

  override fun <A : BasicFileAttributes> readAttributes(
    path: Path,
    type: Class<A>,
    vararg options: LinkOption
  ): A = retryOnClosedFs(path) { p ->
    delegateProvider.readAttributes(p, type, *options)
  }

  override fun readAttributes(
    path: Path,
    attributes: String,
    vararg options: LinkOption
  ): Map<String, Any> = retryOnClosedFs(path) { p ->
    delegateProvider.readAttributes(p, attributes, *options)
  }

  override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption): Unit =
    delegateProvider.setAttribute(path.unwrapped, attribute, value, *options)

  private fun URI.toPath(): Path {
    return delegateProvider.getPath(this)
  }

  private fun FsHandlerPath.reopen() = fileSystem.getPath(delegatePath.toString())

  // Bounds reopen retries so an evicted FsHandleFileSystem (referenceCount < 0,
  // can no longer reopen) surfaces as ClosedFileSystemException instead of StackOverflowError.
  private val Path.unwrapped: Path
    get() {
      if (this !is FsHandlerPath) return this
      var current: FsHandlerPath = this
      repeat(MAX_REOPEN_ATTEMPTS) {
        if (!current.delegatePath.fileSystem.isClosed) {
          return current.delegatePath
        }
        val reopened = current.reopen()
        if (reopened !is FsHandlerPath) return reopened
        current = reopened
      }
      throw ClosedFileSystemException()
    }

  // Handles the race where the delegate FS is closed between `unwrapped`'s isClosed check and the NIO call.
  private inline fun <T> retryOnClosedFs(path: Path, op: (Path) -> T): T = try {
    op(path.unwrapped)
  } catch (_: ClosedFileSystemException) {
    op(path.unwrapped)
  }
}

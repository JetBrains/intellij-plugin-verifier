/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fs

import com.jetbrains.plugin.structure.base.fs.isClosed
import com.jetbrains.plugin.structure.jar.FsHandleFileSystem
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.AccessMode
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

class FsHandlerFileSystemProvider(val delegateProvider: FileSystemProvider) : FileSystemProvider() {
  override fun getScheme(): String? = delegateProvider.scheme

  override fun newFileSystem(uri: URI, env: Map<String, *>): FileSystem =
    FsHandleFileSystem(delegateProvider.newFileSystem(uri, env))

  override fun getFileSystem(uri: URI): FsHandleFileSystem = FsHandleFileSystem(
    delegateProvider.getFileSystem(uri))

  override fun getPath(uri: URI): Path {
    val fs = getFileSystem(uri)
    val path = delegateProvider.getPath(uri)
    return FsHandlerPath(fs, path)
  }

  override fun newByteChannel(
    path: Path,
    options: Set<OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel = delegateProvider.newByteChannel(path.unwrapped, options, *attrs)

  override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>): DirectoryStream<Path> =
    delegateProvider.newDirectoryStream(dir.unwrapped, filter)

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
      path.delegatePath.fileSystem.provider().isSameFile(path, path2)
    } else {
      delegateProvider.isSameFile(path, path2)
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
  ): A = delegateProvider.readAttributes(path.unwrapped, type, *options)

  override fun readAttributes(
    path: Path,
    attributes: String,
    vararg options: LinkOption
  ): Map<String, Any> = delegateProvider.readAttributes(path.unwrapped, attributes, *options)

  override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption): Unit =
    delegateProvider.setAttribute(path.unwrapped, attribute, value, *options)

  private val Path.unwrapped: Path
    get() = if (this is FsHandlerPath) {
      if (delegatePath.fileSystem.isClosed) {
        reopen().unwrapped
      } else {
        delegatePath
      }
    } else {
      this
    }
}
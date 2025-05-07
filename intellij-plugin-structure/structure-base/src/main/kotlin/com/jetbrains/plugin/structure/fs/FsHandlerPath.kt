/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fs

import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService

class FsHandlerPath(private val fileSystem: FileSystem, val delegatePath: Path) : Path {
  override fun getFileSystem(): FileSystem = fileSystem

  override fun isAbsolute() = delegatePath.isAbsolute

  override fun getRoot() = delegatePath.root.wrap()

  override fun getFileName() = delegatePath.fileName.wrap()

  override fun getParent() = delegatePath.parent.wrap()

  override fun getNameCount() = delegatePath.nameCount

  override fun getName(index: Int) = delegatePath.getName(index).wrapNotNull()

  override fun subpath(beginIndex: Int, endIndex: Int) = delegatePath.subpath(beginIndex, endIndex).wrapNotNull()

  override fun startsWith(other: Path) = delegatePath.startsWith(other.unwrapped)

  override fun endsWith(other: Path) = delegatePath.endsWith(other.unwrapped)

  override fun normalize() = delegatePath.normalize().wrapNotNull()

  override fun resolve(other: Path) = delegatePath.resolve(other.unwrapped).wrapNotNull()

  override fun relativize(other: Path) = delegatePath.relativize(other.unwrapped).wrapNotNull()

  override fun toUri(): URI = delegatePath.toUri()

  override fun toAbsolutePath() = delegatePath.toAbsolutePath().wrapNotNull()

  override fun toRealPath(vararg options: LinkOption): FsHandlerPath = delegatePath.toRealPath(*options).wrapNotNull()

  override fun register(watcher: WatchService,
    events: Array<out WatchEvent.Kind<*>>?,
    vararg modifiers: WatchEvent.Modifier?
  ): WatchKey = delegatePath.register(watcher, events, *modifiers)

  override fun compareTo(other: Path) = delegatePath.compareTo(other.unwrapped)

  private fun Path.wrapNotNull(): FsHandlerPath = FsHandlerPath(this@FsHandlerPath.fileSystem, this)

  private fun Path?.wrap(): FsHandlerPath? = this?.let {
    FsHandlerPath(this@FsHandlerPath.fileSystem, it)
  }

  private val Path.unwrapped: Path
    get() = (this as? FsHandlerPath)?.delegatePath ?: this

  override fun toString() = delegatePath.toString()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Path) return false

    val unwrappedOther = other.unwrapped
    return delegatePath == unwrappedOther
  }

  override fun hashCode(): Int = delegatePath.hashCode()
}

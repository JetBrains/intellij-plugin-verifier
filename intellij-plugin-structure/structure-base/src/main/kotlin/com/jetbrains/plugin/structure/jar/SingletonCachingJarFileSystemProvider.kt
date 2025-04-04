/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import java.nio.file.FileSystem
import java.nio.file.Path

object SingletonCachingJarFileSystemProvider : JarFileSystemProvider, AutoCloseable {
  private val delegate = CachingJarFileSystemProvider()

  override fun getFileSystem(jarPath: Path): FileSystem = delegate.getFileSystem(jarPath)

  override fun close(jarPath: Path): Unit = delegate.close(jarPath)

  override fun close(): Unit = delegate.close()
}
package com.jetbrains.plugin.structure.jar

import java.nio.file.FileSystem
import java.nio.file.Path

inline fun <T> Path.useFileSystem(fileSystemProvider: JarFileSystemProvider, useFileSystem: (FileSystem) -> T): T {
  var fs: FileSystem? = null
  return try {
    fs = fileSystemProvider.getFileSystem(this)
    useFileSystem(fs)
  } catch (e: Throwable) {
    throw JarFileSystemProviderException(
      "Path '$this' cannot be used:" + e.message + ". Provider '${fileSystemProvider.javaClass.name}')",
      path = this,
      fileSystemProvider,
      e
    )
  } finally {
    fs?.close()
  }
}

inline operator fun <T> JarFileSystemProvider.invoke(path: Path, useFileSystem: (FileSystem) -> T): T {
  return path.useFileSystem(this, useFileSystem)
}

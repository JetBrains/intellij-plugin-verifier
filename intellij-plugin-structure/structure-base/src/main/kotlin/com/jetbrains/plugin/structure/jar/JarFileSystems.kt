package com.jetbrains.plugin.structure.jar

import java.nio.file.FileSystem
import java.nio.file.Path

fun <T> Path.useFileSystem(fileSystemProvider: JarFileSystemProvider, useFileSystem: (FileSystem) -> T): T {
  return try {
    val fs = fileSystemProvider.getFileSystem(this)
    useFileSystem(fs)
  } catch (e: Throwable) {
    throw e
  } finally {
    fileSystemProvider.close(jarPath = this)
  }
}

operator fun <T> JarFileSystemProvider.invoke(path: Path, useFileSystem: (FileSystem) -> T): T {
  return path.useFileSystem(this, useFileSystem)
}

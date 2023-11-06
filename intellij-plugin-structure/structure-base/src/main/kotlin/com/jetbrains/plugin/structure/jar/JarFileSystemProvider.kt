package com.jetbrains.plugin.structure.jar

import java.nio.file.FileSystem
import java.nio.file.Path

interface JarFileSystemProvider {
  @Throws(JarArchiveException::class)
  fun getFileSystem(jarPath: Path): FileSystem
}

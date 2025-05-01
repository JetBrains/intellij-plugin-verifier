package com.jetbrains.plugin.structure.jar

import java.nio.file.FileSystem
import java.nio.file.Path

interface JarFileSystemProvider {
  @Throws(JarArchiveException::class)
  fun getFileSystem(jarPath: Path): FileSystem

  @Throws(JarArchiveException::class)
  fun getFileSystem(jarPath: Path, configuration: Configuration): FileSystem = getFileSystem(jarPath)

  data class Configuration(val expectedClients: Int = DEFAULT_EXPECTED_CLIENTS)

  companion object {
    const val DEFAULT_EXPECTED_CLIENTS = 1
  }
}

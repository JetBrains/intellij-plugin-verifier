package com.jetbrains.plugin.structure.jar

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

/**
 * Provider that always returns a new instance of filesystem.
 * @see [FileSystems#newFileSystem]
 */
class DefaultJarFileSystemProvider : JarFileSystemProvider {
  @Throws(JarArchiveCannotBeOpenException::class)
  override fun getFileSystem(jarPath: Path): FileSystem {
    return try {
      FileSystems.newFileSystem(jarPath, PluginJar::class.java.classLoader)
    } catch (e: Throwable) {
      throw JarArchiveCannotBeOpenException(jarPath, e)
    }
  }
}

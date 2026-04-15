package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.extension
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystemAlreadyExistsException
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path

class UriJarFileSystemProvider(private val pathToUri: (Path) -> URI = { it.toUri() }) : JarFileSystemProvider {
  private val log: Logger = LoggerFactory.getLogger(UriJarFileSystemProvider::class.java)

  @Throws(JarArchiveCannotBeOpenException::class)
  override fun getFileSystem(jarPath: Path): FileSystem {
    val jarUri = pathToUri(jarPath)
    return try {
      if (!jarPath.isZipOrJar()) {
        throw JarArchiveCannotBeOpenException(jarPath, "must end with '.zip' or '.jar'")
      }
      try {
        FileSystems.getFileSystem(jarUri).also {
          log.debug("Reusing JAR filesystem from JVM cache <{}>", jarUri)
        }
      } catch (_: FileSystemNotFoundException) {
        try {
          FileSystems.newFileSystem(jarUri, emptyMap<String, Any>()).also {
            log.debug("JAR filesystem not found. Creating a new one for <{}>", jarUri)
          }
        } catch (_: FileSystemAlreadyExistsException) {
          FileSystems.getFileSystem(jarUri).also {
            log.debug("Reusing JAR filesystem from JVM cache <{}> (2nd attempt)", jarUri)
          }
        }
      }
    } catch (e: JarArchiveCannotBeOpenException) {
      throw e
    } catch (e: Throwable) {
      throw JarArchiveCannotBeOpenException(jarUri, e)
    }
  }

  private fun Path.isZipOrJar() = extension in listOf("zip", "jar")
}
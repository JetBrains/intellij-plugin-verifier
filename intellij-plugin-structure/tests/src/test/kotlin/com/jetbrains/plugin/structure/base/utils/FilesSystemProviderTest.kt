package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.decompress.ZipCompressor
import com.jetbrains.plugin.structure.jar.CachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.JAR_SCHEME
import com.jetbrains.plugin.structure.jar.JarArchiveCannotBeOpenException
import com.jetbrains.plugin.structure.rules.FileSystemAwareTemporaryFolder
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

class FilesSystemProviderTest {
  @Rule
  @JvmField
  val temporaryFolder = FileSystemAwareTemporaryFolder(FileSystemType.IN_MEMORY)

  @Test
  fun `standard JAR file in the regular filesystem is resolved`() {
    val pathValue = "src/test/resources/resolver-jars/sample-jar-with-descriptor.jar"
    val jarPath = Path.of(pathValue)

    val fsProvider = CachingJarFileSystemProvider()
    val fileSystem = fsProvider.getFileSystem(jarPath)

    assertNotNull(fileSystem)
    val resolvedPath = fileSystem.getPath(pathValue)
    assertEquals(jarPath.invariantSeparatorsPathString, resolvedPath.invariantSeparatorsPathString)
  }

  @Test
  fun `plaintext file created dynamically in JimFS is resolved`() {
    val pathValue = "test.txt"
    val newFile = temporaryFolder.newFile(pathValue)
    newFile.writeText("Hello")

    val fsProvider = CachingJarFileSystemProvider()
    assertThrows(JarArchiveCannotBeOpenException::class.java) {
      fsProvider.getFileSystem(newFile)
    }
  }

  @Test
  fun `file system provider is resolving JimFS-based JARs`() {
    val jarPathValue = "test.jar"
    val jarPath = temporaryFolder.newFile(jarPathValue)
    val uncompressedFolder = temporaryFolder.newFolder()
    val helloTxt = uncompressedFolder.resolve("hello.txt")
    helloTxt.writeText("Hello")


    ZipCompressor(jarPath).use {
      it.addDirectory(uncompressedFolder)
    }

    val fsProvider = CachingJarFileSystemProvider()
    val fileSystem = fsProvider.getFileSystem(jarPath)

    assertNotNull(fileSystem)
    val resolvedJarPath = fileSystem.getPath(jarPathValue)
    assertEquals(jarPathValue, resolvedJarPath.toString())
  }

  @Test
  fun `resolve jar-jimfs-schema via NIO FileSystems`() {
    val jarPathValue = "test.jar"
    val jarPath = temporaryFolder.newFile(jarPathValue)
    val uncompressedFolder = temporaryFolder.newFolder()
    val helloTxt = uncompressedFolder.resolve("hello.txt")
    helloTxt.writeText("Hello")

    ZipCompressor(jarPath).use {
      it.addDirectory(uncompressedFolder)
    }

    val jarJimFsUri = jarPath.toUri()
    assertTrue(jarJimFsUri.scheme == "jimfs")

    val jarAndJimFsUri = URI("$JAR_SCHEME:$jarJimFsUri")
    val jarFileSystem = FileSystems.newFileSystem(jarAndJimFsUri, emptyMap<String, Any>())
    assertNotNull(jarFileSystem)
    val helloTxtInJar = jarFileSystem.getPath("/hello.txt")
    println(helloTxtInJar)
    assertTrue(Files.exists(helloTxt))
  }
}
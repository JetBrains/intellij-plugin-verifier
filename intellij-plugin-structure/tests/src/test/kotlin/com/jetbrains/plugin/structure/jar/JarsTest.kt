package com.jetbrains.plugin.structure.jar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

private const val JAR_FILE_SCHEMA_PREFIX = "$JAR_FILE_SCHEMA:"

class JarsTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `path with file scheme that is absolute and exists`() {
    val existingPath = temporaryFolder.newFile("existing.jar").toPath()

    val jarUri = existingPath.toJarFileUri()
    assertEquals(JAR_SCHEME, jarUri.scheme)
    assertTrue(jarUri.toString().startsWith(JAR_FILE_SCHEMA_PREFIX))
    assertTrue(jarUri.toString().contains(existingPath.toRealPath().toSystemIndependentString()))
  }

  @Test
  fun `path with 'file' scheme that is absolute but doesn't exist`() {
    val nonExistingPath = Paths.get(temporaryFolder.root.absolutePath, "non-existing.jar")
    val jarUri = nonExistingPath.toJarFileUri()

    assertEquals(JAR_SCHEME, jarUri.scheme)
    assertTrue(jarUri.toString().startsWith(JAR_FILE_SCHEMA_PREFIX))
    assertTrue(jarUri.toString().contains(nonExistingPath.normalize().toSystemIndependentString()))
  }

  @Test
  fun `relative path with 'scheme' file that exists is converted to JAR-protocol URI with real path`() {
    val existingPath = temporaryFolder.newFile("relative-existing.jar").toPath()

    val currentDir = Paths.get("").toAbsolutePath()
    val relativePath = currentDir.relativize(existingPath)

    val jarUri = relativePath.toJarFileUri()

    assertEquals(JAR_SCHEME, jarUri.scheme)
    assertTrue(jarUri.toString().startsWith(JAR_FILE_SCHEMA_PREFIX))
    assertTrue(jarUri.toString().contains(existingPath.toRealPath().toSystemIndependentString()))
  }

  @Test
  fun `relative path with 'file' scheme that does not exist is converted to JAR-protocol URI with normalized absolute path`() {
    val relativePath = Paths.get("non-existing-relative.jar")
    val jarUri = relativePath.toJarFileUri()

    assertEquals(JAR_SCHEME, jarUri.scheme)
    assertTrue(jarUri.toString().startsWith(JAR_FILE_SCHEMA_PREFIX))
    val absolutePath = relativePath.toAbsolutePath().normalize()
    assertTrue(jarUri.toString().contains(absolutePath.toSystemIndependentString()))
  }

  @Test
  fun `non-file scheme URI is returned as-is`() {
    val mockUri = URI("https://example.com/file.jar")
    val mockPath = object : Path {
      override fun toUri(): URI = mockUri

      // Implement other required methods with minimal functionality
      override fun getFileSystem() = throw UnsupportedOperationException("Not implemented")
      override fun isAbsolute() = true
      override fun getRoot() = null
      override fun getFileName() = null
      override fun getParent() = null
      override fun getNameCount() = 0
      override fun getName(index: Int) = throw UnsupportedOperationException("Not implemented")
      override fun subpath(beginIndex: Int, endIndex: Int) = throw UnsupportedOperationException("Not implemented")
      override fun startsWith(other: Path) = false
      override fun startsWith(other: String) = false
      override fun endsWith(other: Path) = false
      override fun endsWith(other: String) = false
      override fun normalize() = this
      override fun resolve(other: Path) = throw UnsupportedOperationException("Not implemented")
      override fun resolve(other: String) = throw UnsupportedOperationException("Not implemented")
      override fun resolveSibling(other: Path) = throw UnsupportedOperationException("Not implemented")
      override fun resolveSibling(other: String) = throw UnsupportedOperationException("Not implemented")
      override fun relativize(other: Path) = throw UnsupportedOperationException("Not implemented")
      override fun toAbsolutePath() = this
      override fun toRealPath(vararg options: java.nio.file.LinkOption) = this
      override fun toFile() = throw UnsupportedOperationException("Not implemented")
      override fun register(watcher: java.nio.file.WatchService, events: Array<out java.nio.file.WatchEvent.Kind<*>>, vararg modifiers: java.nio.file.WatchEvent.Modifier) = throw UnsupportedOperationException("Not implemented")
      override fun register(watcher: java.nio.file.WatchService, vararg events: java.nio.file.WatchEvent.Kind<*>) = throw UnsupportedOperationException("Not implemented")
      override fun iterator() = throw UnsupportedOperationException("Not implemented")
      override fun compareTo(other: Path) = 0
    }

    val jarUri = mockPath.toJarFileUri()

    assertEquals("https", jarUri.scheme)
    assertEquals(mockUri, jarUri)
  }

  private fun Path.toSystemIndependentString(): String {
    return toString().replace(File.separatorChar, '/')
  }
}
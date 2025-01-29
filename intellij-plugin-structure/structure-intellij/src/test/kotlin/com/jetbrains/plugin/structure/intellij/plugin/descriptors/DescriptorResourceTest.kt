package com.jetbrains.plugin.structure.intellij.plugin.descriptors

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Path

class DescriptorResourceTest {
  @Test
  fun `JAR-based URI is parsed`() {
    val uri = URI("jar:file:/some/absolute/path/to/plugin.jar!/META-INF/plugin.xml")
    val descriptor = DescriptorResource(ByteArrayInputStream(ByteArray(0)), uri)
    assertEquals("plugin.xml", descriptor.fileName)
    assertEquals(Path.of("plugin.xml"), descriptor.filePath)
    assertEquals("/some/absolute/path/to/plugin.jar", descriptor.artifactFileName)
  }

  @Test
  fun `JAR-based URI with two slashes is parsed`() {
    val uri = URI("jar:file:///some/absolute/path/to/plugin.jar!/META-INF/plugin.xml")
    val descriptor = DescriptorResource(ByteArrayInputStream(ByteArray(0)), uri)
    assertEquals("plugin.xml", descriptor.fileName)
    assertEquals("/some/absolute/path/to/plugin.jar", descriptor.artifactFileName)
  }

  @Test
  fun `JAR-based URI on Windows with two slashes is parsed`() {
    val uri = URI("jar:file://C:/some/absolute/path/to/plugin.jar!/META-INF/plugin.xml")
    val descriptor = DescriptorResource(ByteArrayInputStream(ByteArray(0)), uri)
    assertEquals("plugin.xml", descriptor.fileName)
    assertEquals("C:/some/absolute/path/to/plugin.jar", descriptor.artifactFileName)
  }

  @Test
  fun `file-based URI is parsed`() {
    val uri = URI("file:///some/absolute/path/to/plugin.jar!/META-INF/plugin.xml")
    val descriptor = DescriptorResource(ByteArrayInputStream(ByteArray(0)), uri)
    assertEquals("plugin.xml", descriptor.fileName)
  }
}
package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.CachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.JarArchiveException
import com.jetbrains.plugin.structure.jar.META_INF
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import com.jetbrains.plugin.structure.jar.PluginJar
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Path


class PluginJarTest {
  private val nonexistentFileName = "!n0n3xist3nt.jar"
  private val nonexistentPath = Path.of(nonexistentFileName)

  private val jarPath = Path.of("src/test/resources/resolver-jars/sample-jar-with-descriptor.jar")

  @Test
  fun `descriptor path is resolved`() {
    PluginJar(jarPath).use { jar ->
      val descriptorPath = jar.resolveDescriptorPath()
      assertEquals("META-INF/plugin.xml", descriptorPath.toString())
    }
  }

  @Test
  fun `descriptor path is resolved with explicit path`() {
    PluginJar(jarPath).use { jar ->
      val descriptorPath = jar.resolveDescriptorPath(META_INF + File.separator + PLUGIN_XML)
      assertEquals("META-INF/plugin.xml", descriptorPath.toString())
    }
  }

  @Test
  fun `plugin descriptor is open as a reader`() {
    PluginJar(jarPath).use { jar ->
      val reader = jar.openDescriptor()
      if (reader == null) {
        fail("Plugin descriptor reader cannot be open. Reader must be non-null")
        return
      }
      assertTrue(reader.readText().isNotEmpty())
    }
  }

  @Test
  fun `plugin descriptor is open as a result`() {
    PluginJar(jarPath).use { jar ->
      val result = jar.getPluginDescriptor()
      if (result is Found) {
        assertTrue(result.reader.readText().isNotEmpty())
      } else {
        fail("Plugin descriptor reader cannot be open. Reader must be non-null")
        return
      }
    }
  }

  @Test
  fun `nonexistent plugin JAR path is provided`() {
    val jarArchiveException = assertThrows(JarArchiveException::class.java) {
      PluginJar(nonexistentPath)
    }
    assertEquals("JAR file cannot be open at [!n0n3xist3nt.jar]", jarArchiveException.message)
  }

  @Test
  fun `nonexistent plugin descriptor cannot be open as a reader`() {
    PluginJar(jarPath).use { jar ->
      val reader = jar.openDescriptor("nonexistent-descriptor.xml")
      assertNull(reader)
    }
  }

  @Test
  fun `descriptor path is resolved with different FS provider`() {
    val fsProvider = CachingJarFileSystemProvider()
    PluginJar(jarPath, fsProvider).use { jar ->
      val descriptorPath = jar.resolveDescriptorPath()
      assertEquals("META-INF/plugin.xml", descriptorPath.toString())
    }
  }

  @Test
  fun `descriptor path is resolved with different FS provider and nonexistent file`() {
    val fsProvider = CachingJarFileSystemProvider()
    PluginJar(jarPath, fsProvider).use { jar ->
      val descriptorPath = jar.resolveDescriptorPath(nonexistentFileName)
      assertNull(descriptorPath)
    }
  }
}
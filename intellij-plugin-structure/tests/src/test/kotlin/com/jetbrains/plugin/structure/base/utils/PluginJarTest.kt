package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.*
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.nio.file.Path


@RunWith(Parameterized::class)
class PluginJarTest(private val fileSystemProvider: JarFileSystemProvider) {
  private val nonexistentFileName = "!n0n3xist3nt.jar"
  private val nonexistentPath = Path.of(nonexistentFileName)

  private val jarPath = Path.of("src/test/resources/resolver-jars/sample-jar-with-descriptor.jar")

  companion object {
    @Parameters
    @JvmStatic
    fun fsProviders(): Array<Array<JarFileSystemProvider>> {
      return arrayOf(
        arrayOf(DefaultJarFileSystemProvider()),
        arrayOf(CachingJarFileSystemProvider()),
        arrayOf(SingletonCachingJarFileSystemProvider)
      )
    }
  }

  @Test
  fun `descriptor path is resolved`() {
    PluginJar(jarPath, fileSystemProvider).use { jar ->
      val descriptorPath = jar.resolveDescriptorPath()
      assertEquals("META-INF/plugin.xml", descriptorPath.toString())
    }
  }

  @Test
  fun `descriptor path is resolved with explicit path`() {
    PluginJar(jarPath, fileSystemProvider).use { jar ->
      val descriptorPath = jar.resolveDescriptorPath(META_INF + File.separator + PLUGIN_XML)
      assertEquals("META-INF/plugin.xml", descriptorPath.toString())
    }
  }

  @Test
  fun `plugin descriptor is open as a result`() {
    PluginJar(jarPath, fileSystemProvider).use { jar ->
      val result = jar.getPluginDescriptor()
      if (result is Found) {
        assertTrue(result.inputStream.bufferedReader().readText().isNotEmpty())
      } else {
        fail("Plugin descriptor reader cannot be open. Reader must be non-null")
        return
      }
    }
  }

  @Test
  fun `nonexistent plugin JAR path is provided`() {
    val jarArchiveException = assertThrows(JarArchiveException::class.java) {
      PluginJar(nonexistentPath, fileSystemProvider)
    }
    assertThat(jarArchiveException.message, containsString("JAR file cannot be open at [!n0n3xist3nt.jar]"))
  }

  @Test
  fun `nonexistent plugin descriptor cannot be resolved`() {
    PluginJar(jarPath, fileSystemProvider).use { jar ->
      val pluginDescriptor = jar.getPluginDescriptor("nonexistent-descriptor.xml")
      assertTrue(pluginDescriptor is PluginDescriptorResult.NotFound)
    }
  }
}
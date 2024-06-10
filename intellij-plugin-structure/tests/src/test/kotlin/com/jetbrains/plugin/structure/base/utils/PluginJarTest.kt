package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.CachingJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.DefaultJarFileSystemProvider
import com.jetbrains.plugin.structure.jar.JarArchiveException
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.META_INF
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import com.jetbrains.plugin.structure.jar.PluginJar
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.File
import java.io.FileNotFoundException
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

  @Test
  fun `descriptor in the resource root is chosen from multiple path candidates`() {
    val pluginJarPath = getPluginJarPath("sample-jar-with-descriptor-in-resource-root.jar")

    PluginJar(pluginJarPath, fileSystemProvider).use { jar ->
      val pluginDescriptor = jar.getPluginDescriptor("META-INF/plugin.xml", "descriptor.xml")
      assertTrue(pluginDescriptor is Found)
      assertEquals("descriptor.xml", (pluginDescriptor as Found).path.simpleName)
    }
  }

  @Test
  fun `find default and dark icon`() {
    val pluginJarPath = getPluginJarPath("simple-with-default-icon-and-dark-icon.jar")
    val icons = PluginJar(pluginJarPath).getIcons()
    assertEquals(2, icons.size)
  }

  @Test
  fun `ignore other icons when default icon is not found`() {
    val pluginJarPath = getPluginJarPath("simple-jar.jar")
    val icons = PluginJar(pluginJarPath).getIcons()
    assertEquals(0, icons.size)
  }

  @Test
  fun `dark icon found but not the default one`() {
    val pluginJarPath = getPluginJarPath("simple-with-dark-icon-and-no-default-icon.jar")

    val icons = PluginJar(pluginJarPath).getIcons()
    assertEquals(0, icons.size)
  }

  fun getPluginJarPath(jarName: String): Path {
    val jarResourceUrl = PluginJar::class.java.getResource("/resolver-jars/$jarName")
    if (jarResourceUrl === null) {
      throw FileNotFoundException("JAR $jarName cannot be resolved in the filesystem")
    }
    return Path.of(jarResourceUrl.toURI())
  }
}
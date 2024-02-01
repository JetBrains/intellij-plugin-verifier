package com.jetbrains.plugin.structure.base.utils

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.DefaultJarFileSystemProvider
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class PluginArtifactIoTest {
  private val jarFileName = "sample-jar-with-descriptor.jar"
  private val pluginJarPath = Path.of("src/test/resources/resolver-jars/$jarFileName")

  private val zipPluginFileName = "sample-zip-plugin-with-descriptor.zip"
  private val pluginZipPath = Path.of("src/test/resources/resolver-jars/$zipPluginFileName")

  @Test
  fun `plugin created from JimFS is properly resolved`() {
    pluginJarPath.inputStream().use { pluginJarInputStream ->
      Jimfs.newFileSystem(Configuration.unix()).use { fs ->
        val jimFsJarPath = fs.getPath(jarFileName)
        Files.copy(pluginJarInputStream, jimFsJarPath)

        val pluginCreationResult = IdePluginManager.createManager().createPlugin(jimFsJarPath)
        assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
      }
    }
  }

  @Test
  fun `plugin created from ZIP in JimFS is properly resolved`() {
    pluginZipPath.inputStream().use { pluginZipInputStream ->
      Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
        val tmpDir = jimFs.getPath("/tmp")
        Files.createDirectory(tmpDir)
        val jimFsZipPath = tmpDir.resolve(zipPluginFileName).toAbsolutePath()
        Files.copy(pluginZipInputStream, jimFsZipPath)

        val pluginCreationResult = IdePluginManager.createManager(tmpDir).createPlugin(jimFsZipPath)
        assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
      }
    }
  }

  @Test
  fun `plugin created from ZIP in JimFS and custom JAR filesystem provider is properly resolved`() {
    pluginZipPath.inputStream().use { pluginZipInputStream ->
      Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
        val tmpDir = jimFs.getPath("/tmp")
        Files.createDirectory(tmpDir)
        val jimFsZipPath = tmpDir.resolve(zipPluginFileName).toAbsolutePath()
        Files.copy(pluginZipInputStream, jimFsZipPath)

        val pluginManager = IdePluginManager.createManager(DefaultResourceResolver, tmpDir, DefaultJarFileSystemProvider())
        val pluginCreationResult = pluginManager.createPlugin(jimFsZipPath)
        assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
      }
    }
  }

  @Test
  fun `plugin created from ZIP properly resolved`() {
      val pluginCreationResult = IdePluginManager.createManager().createPlugin(pluginZipPath)
    assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
  }
}

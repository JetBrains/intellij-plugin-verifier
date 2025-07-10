package com.jetbrains.plugin.structure.base.utils

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.createIdePluginManager
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
      Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
        val pluginDir = jimFs.getPath("/plugins").also { it.createDir() }
        val jimFsJarPath = pluginDir.resolve(jarFileName).toAbsolutePath()
        Files.copy(pluginJarInputStream, jimFsJarPath)

        val extractedPluginsDir = jimFs.getPath("/extracted-plugins").also { it.createDir() }
        PluginArchiveManager(extractedPluginsDir).use { archiveManager ->
          val pluginCreationResult = createIdePluginManager(archiveManager).createPlugin(jimFsJarPath)
          assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
        }
      }
    }
  }

  @Test
  fun `plugin created from ZIP in JimFS is properly resolved`() {
    pluginZipPath.inputStream().use { pluginZipInputStream ->
      Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
        val pluginDir = jimFs.getPath("/plugins").also { it.createDir() }
        val extractedPluginsDir = jimFs.getPath("/extracted-plugins").also { it.createDir() }
        val jimFsZipPath = pluginDir.resolve(zipPluginFileName).toAbsolutePath()
        Files.copy(pluginZipInputStream, jimFsZipPath)
        PluginArchiveManager(extractedPluginsDir).use { archiveManager ->
          val pluginCreationResult = createIdePluginManager(archiveManager).createPlugin(jimFsZipPath)
          assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
        }
      }
    }
  }

  @Test
  fun `plugin created from ZIP in JimFS and custom JAR filesystem provider is properly resolved`() {
    pluginZipPath.inputStream().use { pluginZipInputStream ->
      Jimfs.newFileSystem(Configuration.unix()).use { jimFs ->
        val pluginDir = jimFs.getPath("/plugins").also { it.createDir() }
        val extractedPluginsDir = jimFs.getPath("/extracted-plugins").also { it.createDir() }
        val jimFsZipPath = pluginDir.resolve(zipPluginFileName).toAbsolutePath()
        Files.copy(pluginZipInputStream, jimFsZipPath)

        PluginArchiveManager(extractedPluginsDir).use { archiveManager ->
          val pluginManager = createIdePluginManager {
            resourceResolver = DefaultResourceResolver
            pluginArchiveManager = archiveManager
            fileSystemProvider = DefaultJarFileSystemProvider()
          }
          val pluginCreationResult = pluginManager.createPlugin(jimFsZipPath)
          assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
        }
      }
    }
  }

  @Test
  fun `plugin created from ZIP properly resolved`() {
    PluginArchiveManager(Settings.EXTRACT_DIRECTORY.getAsPath()).use { archiveManager ->
      val pluginCreationResult = createIdePluginManager(archiveManager).createPlugin(pluginZipPath)
      assertThat(pluginCreationResult, instanceOf(PluginCreationSuccess::class.java))
    }
  }
}

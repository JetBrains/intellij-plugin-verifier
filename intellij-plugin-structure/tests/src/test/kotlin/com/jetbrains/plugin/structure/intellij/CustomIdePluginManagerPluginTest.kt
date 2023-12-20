package com.jetbrains.plugin.structure.intellij

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.DefaultJarFileSystemProvider
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.nio.file.Path

class CustomIdePluginManagerPluginTest {
  private val jarPath = Path.of("src/test/resources/resolver-jars/sample-jar-with-descriptor.jar")

  @Test
  fun `IDE plugin manager is created with explicit JAR file system provider`() {
    val pluginManager = IdePluginManager.createManager(DefaultResourceResolver, Settings.EXTRACT_DIRECTORY.getAsPath(), DefaultJarFileSystemProvider())
    val pluginCreationResult = pluginManager.createPlugin(jarPath)
    assertThat(pluginCreationResult, CoreMatchers.instanceOf(PluginCreationSuccess::class.java))
  }
}
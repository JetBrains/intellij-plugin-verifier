package com.jetbrains.plugin.structure.intellij.plugin.loaders

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.problems.IncorrectJarOrDirectory
import com.jetbrains.plugin.structure.intellij.plugin.createZip
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class JarOrDirectoryPluginLoaderTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `plugin artifact path is neither JAR nor directory`() {
    val pluginLoaderRegistry = PluginLoaderProvider().apply {
      register(JarOrDirectoryPluginLoader.Context::class.java, JarOrDirectoryPluginLoader(pluginLoaderRegistry = this))
    }
    val loader = JarOrDirectoryPluginLoader(pluginLoaderRegistry)

    val pluginFile: Path = temporaryFolder.newFile("plugin.zip").toPath()
    createZip(pluginFile, mapOf("README.txt" to "A readme file"))

    val context = JarOrDirectoryPluginLoader.Context(
      pluginFile, PLUGIN_XML, validateDescriptor = true, DefaultResourceResolver, parentPlugin = null,
      IntelliJPluginCreationResultResolver(), hasDotNetDirectory = false
    )
    val pluginCreator = loader.loadPlugin(context)
    assertFalse(pluginCreator.isSuccess)
    val creationResult = pluginCreator.pluginCreationResult
    assertTrue(creationResult is PluginCreationFail)
    creationResult as PluginCreationFail
    with(creationResult.errorsAndWarnings) {
      assertEquals(1, size)
      val problem = first()
      assertTrue(problem is IncorrectJarOrDirectory)
      problem as IncorrectJarOrDirectory
      assertEquals(
        "The plugin artifact path must be a .jar archive or a directory, but was a ZIP file at [$pluginFile]",
        problem.message
      )
    }
  }
}
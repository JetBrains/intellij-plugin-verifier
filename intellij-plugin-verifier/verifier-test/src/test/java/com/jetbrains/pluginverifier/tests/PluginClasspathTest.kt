package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.classes.locator.LibModulesDirectoryLocator
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.createPluginResolver
import com.jetbrains.pluginverifier.tests.mocks.classBytes
import com.jetbrains.pluginverifier.tests.mocks.descriptor
import com.jetbrains.pluginverifier.tests.mocks.ideaPlugin
import net.bytebuddy.ByteBuddy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginClasspathTest : BasePluginTest() {
  private val byteBuddy = ByteBuddy()

  private fun buildPluginInZip(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip").toPath(), pluginContentBuilder)
    val ideManager = IdePluginManager.createManager()
    return ideManager.createPlugin(pluginFile, validateDescriptor = true)
  }

  private fun buildPluginInDirectory(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildDirectory(temporaryFolder.newFolder("plugin").toPath(), pluginContentBuilder)
    val ideManager = IdePluginManager.createManager()
    return ideManager.createPlugin(pluginFile, validateDescriptor = true)
  }

  @Test
  fun `classes in plugin JAR and in plugin lib-modules are discovered`() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath")
    val result = buildPluginInZip {
      dir(pluginId) {
        dir("lib") {
          zip("plugin.jar") {
            descriptor(descriptor)
            classBytes("SomeClass", byteBuddy)
          }
          dir("modules") {
            zip("plugin-module.jar") {
              classBytes("Module", byteBuddy)
            }
          }
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)
    val pluginCreated = result as PluginCreationSuccess

    IdePluginClassesFinder.findPluginClasses(pluginCreated.plugin).use { classLocations ->
      classLocations.createPluginResolver().use {
        val pluginClasses = it.allClasses
        assertEquals(2, pluginClasses.size)
        assertTrue(pluginClasses.containsAll(setOf("SomeClass", "Module")))
      }
    }
  }

  @Test
  fun `lib-modules directory locator discovers classes`() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath")
    val result = buildPluginInDirectory {
      dir("lib") {
        zip("plugin.jar") {
          descriptor(descriptor)
        }
        dir("modules") {
          zip("plugin-module.jar") {
            classBytes("Module", byteBuddy)
          }
        }
      }
    }

    assertTrue(result is PluginCreationSuccess)
    val pluginCreated = result as PluginCreationSuccess
    val plugin = pluginCreated.plugin

    val locator = LibModulesDirectoryLocator(Resolver.ReadMode.FULL)
    val classes = locator.findClasses(plugin, plugin.originalFile!!)
      .flatMap { it.allClasses }
    assertEquals(1, classes.size)
    assertEquals("Module", classes.first())
  }
}
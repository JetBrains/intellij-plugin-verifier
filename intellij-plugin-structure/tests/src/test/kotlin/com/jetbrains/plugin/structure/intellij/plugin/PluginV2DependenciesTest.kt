/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.MockIdeBuilder
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager
import junit.framework.TestCase.assertEquals
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

private const val PLUGIN_JAR_NAME = "plugin.jar"

class PluginV2DependenciesTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  @Before
  fun setUp() {
    ideRoot = MockIdeBuilder(temporaryFolder).buildIdeaDirectory()
  }

  @Test
  fun `plugin has content modules, when any of the module dependencies are missing, the whole module is optional`() {
    val ideManager = ProductInfoBasedIdeManager()
    val ide = ideManager.createIde(ideRoot)
    /*
      The plugin consists of <content> modules and dependencies of these modules are optional.
      If the dependent module or plugin (of such a module) doesn't exist in the IDE, this module won't be loaded.
     */
    val plugin = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              <id>somePlugin</id>
              <name>someName</name>
              <version>someVersion</version>
              <vendor email="vendor.com" url="url">Some Vendor</vendor>
              <description>this description is looooooooooong enough</description>
              <change-notes>these change-notes are looooooooooong enough</change-notes>
              <idea-version since-build="242"/>
              <depends>com.intellij.modules.platform</depends>
              
              <content>
                <module name="somePlugin.moduleOne" />
                <module name="somePlugin.moduleTwo" />
              </content>
              
            </idea-plugin>
            """.trimIndent()
        }
      }
      file("somePlugin.moduleOne.xml", """
        <idea-plugin package="somePlugin.moduleOne">
          <dependencies>
            <!-- module is present in the IDE -->
            <module name="intellij.notebooks.ui" />
          </dependencies>
        </idea-plugin>
      """.trimIndent())
      file("somePlugin.moduleTwo.xml", """
        <idea-plugin package="somePlugin.moduleTwo">
          <dependencies>
            <!-- plugin is unavailable in the IDE -->
            <plugin id="com.jetbrains.rust" />
          </dependencies>
        </idea-plugin>
      """.trimIndent())
    }

    with(plugin.plugin.modulesDescriptors) {
      assertEquals(2, size)
      val moduleOne = first { it.name == "somePlugin.moduleOne" }
      val moduleTwo = first { it.name == "somePlugin.moduleTwo" }
      assertFalse(
        "Module 'somePlugin.moduleOne' expected to be mandatory, as all dependencies are present in the IDE",
        isOptional(ide, moduleOne)
      )
      assertTrue(
        "Module 'somePlugin.moduleTwo' expected to be optional, as dependencies are not available in the IDE",
        isOptional(ide, moduleTwo)
      )
    }

    assertTrue(
      "All content module dependencies are optional in the plugin",
      plugin.plugin.dependencies.filter { it is ModuleV2Dependency || it is PluginV2Dependency }
        .all { it.isOptional })
  }

  private fun isOptional(ide: Ide, moduleDescriptor: ModuleDescriptor) = !ide.containsAllDependencies(moduleDescriptor)

  private fun Ide.containsAllDependencies(moduleDescriptor: ModuleDescriptor): Boolean {
    return moduleDescriptor.dependencies
      .all { dependency ->
        findPluginByIdOrModuleId(dependency.id) != null
      }
  }

  private fun Ide.findPluginByIdOrModuleId(id: String): IdePlugin? {
    return findPluginById(id)
      ?: findPluginByModule(id)
  }

  private fun buildCorrectPlugin(pluginJarName: String = PLUGIN_JAR_NAME, pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationSuccess<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginJarName, pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationSuccess) {
      fail("This plugin has not been created. Creation failed with error(s).")
    }
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun buildIdePlugin(pluginJarName: String = PLUGIN_JAR_NAME, pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile(pluginJarName).toPath(), pluginContentBuilder)
    return IdePluginManager.createManager().createPlugin(pluginFile)
  }
}
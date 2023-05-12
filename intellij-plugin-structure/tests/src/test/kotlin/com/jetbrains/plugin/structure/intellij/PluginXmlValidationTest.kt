package com.jetbrains.plugin.structure.intellij

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyConfigFileIsEmpty
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyConfigFileNotSpecified
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PluginXmlValidationTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  companion object {

    private const val HEADER = """
      <id>someId</id>
      <name>someName</name>
      <version>someVersion</version>
      ""<vendor email="vendor.com" url="url">vendor</vendor>""
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

  }

  @Test
  fun `plugin declaring optional dependency but missing config-file`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <depends optional="true">com.intellij.bundled.plugin.id</depends>
            </idea-plugin>
          """
        }
      }
    }

    val warnings = pluginCreationSuccess.warnings
    assertEquals(1, warnings.size)
    val warning = warnings.filterIsInstance<OptionalDependencyConfigFileNotSpecified>()
            .firstOrNull()
    if (warning == null) {
      fail("Expected 'Optional Dependency Config File Not Specified' plugin warning")
    }
  }

  @Test
  fun `plugin declaring optional dependency but empty config-file is a creation error`() {
    val pluginCreationFail = buildMalformedPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <depends optional="true" config-file="">com.intellij.optional.plugin.id</depends>
            </idea-plugin>
          """
        }
      }
    }

    assertEquals(1, pluginCreationFail.errorsAndWarnings.size)
    val warning = pluginCreationFail.errorsAndWarnings.filterIsInstance<OptionalDependencyConfigFileIsEmpty>()
            .firstOrNull()
    if (warning == null) {
      fail("Expected 'Optional Dependency Config File Is Empty' plugin warning")
    }
  }

  private fun buildMalformedPlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationFail<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationFail) {
      fail("This plugin should not be created, but is correct.")
    }
    return pluginCreationResult as PluginCreationFail
  }

  private fun buildCorrectPlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationSuccess<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationSuccess) {
      fail("This plugin has not been created. Creation failed with error(s).")
    }
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun buildIdePlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath(), pluginContentBuilder)
    return IdePluginManager.createManager().createPlugin(pluginFile)
  }

  class PluginCreationHandler {
    fun onSuccess(result: PluginCreationSuccess<IdePlugin>): PluginCreationSuccess<IdePlugin> {
      return result
    }
    fun onFailure(result: PluginCreationFail<IdePlugin>): PluginCreationFail<IdePlugin> {
      return result
    }
  }
}
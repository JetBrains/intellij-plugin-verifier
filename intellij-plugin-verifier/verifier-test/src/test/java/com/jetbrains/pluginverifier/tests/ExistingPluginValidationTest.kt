package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory

class ExistingPluginValidationTest : BasePluginTest() {
  @Test
  fun `plugin is not built due to missing ID but such problem is filtered`() {
    val header = header("")
    val problemResolver = object: PluginCreationResultResolver {
      private val logger = LoggerFactory.getLogger("verification.structure")

      override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
        return if (problems.isEmpty()) {
          PluginCreationSuccess(plugin, problems)
        } else {
          problems.forEach {
            logger.info("Plugin problem will be ignored by the problem resolver: $it")
          }
          PluginCreationSuccess(plugin, problems)
        }
      }
    }

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    Assert.assertTrue(result is PluginCreationSuccess)
  }

  private fun header(pluginId: String = "someid") = """
    <id>$pluginId</id>
    <name>someName</name>
    <version>someVersion</version>
    ""<vendor email="vendor.com" url="url">vendor</vendor>""
    <description>this description is looooooooooong enough</description>
    <change-notes>these change-notes are looooooooooong enough</change-notes>
    <idea-version since-build="131.1"/>
  """
}
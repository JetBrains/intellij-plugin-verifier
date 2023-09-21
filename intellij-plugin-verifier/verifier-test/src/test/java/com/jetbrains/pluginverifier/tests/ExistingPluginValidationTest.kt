package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.DelegatingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ExistingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.slf4j.LoggerFactory

class ExistingPluginValidationTest : BasePluginTest() {
  @Test
  fun `plugin is not built due to missing ID but such problem is filtered`() {
    val ideaPlugin = ideaPlugin(pluginId = "")
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = object: DelegatingPluginCreationResultResolver(delegateResolver) {
      private val logger = LoggerFactory.getLogger("verification.structure")

      override fun doResolve(plugin: IdePlugin, problems: List<PluginProblem>): ResolutionResult {
        problems.forEach {
          logger.info("Plugin problem will be ignored by the problem resolver: $it")
        }
        return ResolutionResult.Resolved(PluginCreationSuccess(plugin, emptyList()))
      }
    }

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
            </idea-plugin>
          """
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)
  }

  @Test
  fun `plugin is not built due to unsupported prefix ID but such problem is filtered`() {
    val header = ideaPlugin("com.example")
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = ExistingPluginCreationResultResolver(delegateResolver)

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
    assertTrue(result is PluginCreationSuccess)
  }

  @Test
  fun `plugin is not built due to two problems, one is filtered`() {
    val header = ideaPlugin("com.example", "")
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = ExistingPluginCreationResultResolver(delegateResolver)

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
    assertTrue(result is PluginCreationFail)
    val pluginCreationFail = result as PluginCreationFail
    Assert.assertEquals(1, pluginCreationFail.errorsAndWarnings.size)
  }

  private fun ideaPlugin(pluginId: String = "someid", pluginName: String = "someName") = """
    <id>$pluginId</id>
    <name>$pluginName</name>
    <version>someVersion</version>
    ""<vendor email="vendor.com" url="url">vendor</vendor>""
    <description>this description is looooooooooong enough</description>
    <change-notes>these change-notes are looooooooooong enough</change-notes>
    <idea-version since-build="131.1"/>
  """
}
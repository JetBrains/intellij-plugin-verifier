package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.DelegatingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ExistingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory

class ExistingPluginValidationTest : BasePluginTest() {
  @Test
  fun `plugin is not built due to missing ID but such problem is filtered`() {
    val header = header("")
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = object: DelegatingPluginCreationResultResolver(delegateResolver) {
      private val logger = LoggerFactory.getLogger("verification.structure")

      override fun doResolve(plugin: IdePlugin, problems: List<PluginProblem>): ResolutionResult {
        return if (problems.isEmpty()) {
          ResolutionResult.Resolved(PluginCreationSuccess(plugin, problems))
        } else {
          problems.forEach {
            logger.info("Plugin problem will be ignored by the problem resolver: $it")
          }
          ResolutionResult.Resolved(PluginCreationSuccess(plugin, emptyList()))
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

  @Test
  fun `plugin is not built due to unsupported prefix ID but such problem is filtered`() {
    val header = header("com.example")
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
    Assert.assertTrue(result is PluginCreationSuccess)
  }

  @Test
  fun `plugin is not built due to two problems, one is filtered`() {
    val header = header("com.example", "")
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
    Assert.assertTrue(result is PluginCreationFail)
    val pluginCreationFail = result as PluginCreationFail
    Assert.assertEquals(1, pluginCreationFail.errorsAndWarnings.size)
  }

  private fun header(pluginId: String = "someid", pluginName: String = "someName") = """
    <id>$pluginId</id>
    <name>$pluginName</name>
    <version>someVersion</version>
    ""<vendor email="vendor.com" url="url">vendor</vendor>""
    <description>this description is looooooooooong enough</description>
    <change-notes>these change-notes are looooooooooong enough</change-notes>
    <idea-version since-build="131.1"/>
  """
}
package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.*
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
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
  fun `plugin is not built due to unsupported prefix ID but such problem level is remapped`() {
    val header = ideaPlugin("com.example")
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = LevelRemappingPluginCreationResultResolver(delegateResolver)

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
    val pluginCreated = result as PluginCreationSuccess
    assertEquals(1, pluginCreated.warnings.size)
    val reclassifiedPluginProblem = pluginCreated.warnings.first()
    assertEquals(PluginProblem.Level.WARNING, reclassifiedPluginProblem.level)
    assertTrue(reclassifiedPluginProblem is ReclassifiedPluginProblem)
    assertTrue((reclassifiedPluginProblem as ReclassifiedPluginProblem).unwrapped is IllegalPluginIdPrefix)
  }

  @Test
  fun `plugin is built, it has two different plugin problems and both are remapped`() {
    val erroneousSinceBuild = "1.*"
    val header = ideaPlugin("com.example", sinceBuild = erroneousSinceBuild)
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = LevelRemappingPluginCreationResultResolver(delegateResolver)

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
    assertThat("Plugin Creation Result must succeed", result, instanceOf(PluginCreationSuccess::class.java))
    val pluginCreated = result as PluginCreationSuccess
    assertEquals(2, pluginCreated.warnings.size)
    val reclassifiedProblems = pluginCreated.warnings
      .filter { ReclassifiedPluginProblem::class.isInstance(it) }
      .map { it as ReclassifiedPluginProblem }
      .map { it.unwrapped }

    assertEquals(2, reclassifiedProblems.size)
    assertThat("Reclassified problems contains an 'IllegalPluginIdPrefix' plugin problem ", reclassifiedProblems.find { IllegalPluginIdPrefix::class.isInstance(it) } != null)
    assertThat("Reclassified problems contains an 'InvalidSinceBuild' plugin problem ", reclassifiedProblems.find { InvalidSinceBuild::class.isInstance(it) } != null)
  }


  @Test
  fun `plugin is not built due to two different plugin problems with 'error' severity but one level is remapped`() {
    val erroneousSinceBuild = "1.*"
    val erroneousUntilBuild = "1000"
    val header = ideaPlugin("plugin.with.two.problems", sinceBuild = erroneousSinceBuild, untilBuild = erroneousUntilBuild)
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = LevelRemappingPluginCreationResultResolver(delegateResolver)

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
    val failure = result as PluginCreationFail
    assertEquals(2, failure.errorsAndWarnings.size)
    val reclassifiedProblems = failure.errorsAndWarnings
      .filter { ReclassifiedPluginProblem::class.isInstance(it) }
      .map { it as ReclassifiedPluginProblem }
      .map { it.unwrapped }

    assertEquals(1, reclassifiedProblems.size)
    assertThat("Reclassified problems contains an 'InvalidSinceBuild' plugin problem ", reclassifiedProblems.find { InvalidSinceBuild::class.isInstance(it) } != null)
  }


  private fun ideaPlugin(pluginId: String = "someid", pluginName: String = "someName", sinceBuild: String = "131.1", untilBuild: String = "231.1") = """
    <id>$pluginId</id>
    <name>$pluginName</name>
    <version>someVersion</version>
    ""<vendor email="vendor.com" url="url">vendor</vendor>""
    <description>this description is looooooooooong enough</description>
    <change-notes>these change-notes are looooooooooong enough</change-notes>
    <idea-version since-build="$sinceBuild" until-build="$untilBuild"/>
    <depends>com.intellij.modules.platform</depends>
  """
}
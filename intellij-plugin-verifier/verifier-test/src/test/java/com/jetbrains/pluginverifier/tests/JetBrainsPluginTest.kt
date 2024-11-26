package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionWrongFormat
import com.jetbrains.plugin.structure.intellij.problems.remapping.JsonUrlProblemLevelRemappingManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.options.PluginParsingConfiguration
import com.jetbrains.pluginverifier.options.PluginParsingConfigurationResolution
import org.junit.Assert.assertTrue
import org.junit.Test

class JetBrainsPluginTest : BasePluginTest() {
  @Test
  fun `plugin has a JetBrains vendor and forbidden plugin ID`() {
    val ide = buildIde("IU-242.21829.142")

    val forbiddenPluginId = "org.jetbrains.somePlugin"
    val ideaPlugin = ideaPlugin(pluginId = forbiddenPluginId, vendor = "JetBrains s.r.o.")
    val creationResult = buildPluginWithResult(problemResolver) {
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

    assertSuccess(creationResult) {
      val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
      assertTrue(verificationResult is PluginVerificationResult.Verified)
      val verifiedResult = verificationResult as PluginVerificationResult.Verified

      assertEmpty("Compatibility Problems", verifiedResult.compatibilityProblems)
      assertEmpty("Plugin Structure Warnings", verifiedResult.pluginStructureWarnings)
      assertEmpty("Plugin Warnings", warnings)
    }
  }

  @Test
  fun `plugin has a JetBrains vendor`() {
    val ide = buildIde("IU-242.21829.142")
    val forbiddenPluginId = "org.jetbrains.somePlugin"
    val ideaPlugin = ideaPlugin(pluginId = forbiddenPluginId, vendor = "JetBrains s.r.o.")

    val creationResult = buildPluginWithResult(problemResolver) {
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

    assertSuccess(creationResult) {
      val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
      assertTrue(verificationResult is PluginVerificationResult.Verified)
      val verifiedResult = verificationResult as PluginVerificationResult.Verified

      assertEmpty("Compatibility Problems", verifiedResult.compatibilityProblems)
      assertEmpty("Plugin Structure Warnings", verifiedResult.pluginStructureWarnings)
      assertEmpty("Plugin Warnings", warnings)
    }
  }

  @Test
  fun `plugin has a JetBrains vendor and zero-prefixed release version as a paid plugin`() {
    val ide = buildIde("IU-242.21829.142")

    val pluginId = "org.jetbrains.somePlugin"
    val ideaPlugin = ideaPlugin(pluginId = pluginId, vendor = "JetBrains s.r.o.")
    val creationResult = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
               <product-descriptor code="GZL" release-date="20240626" release-version="03" />
            </idea-plugin>
          """
        }
      }
    }

    assertSuccess(creationResult) {
      val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
      assertTrue(verificationResult is PluginVerificationResult.Verified)
      val verifiedResult = verificationResult as PluginVerificationResult.Verified

      assertEmpty("Compatibility Problems", verifiedResult.compatibilityProblems)
      assertContainsWarning<ReleaseVersionWrongFormat>("Invalid plugin descriptor 'plugin.xml'. The <release-version> parameter (03) format is invalid. Ensure it is an integer with at least two digits.")
    }
  }

  private val problemResolver: PluginCreationResultResolver
    get() {
      val pluginParsingConfigurationResolution = PluginParsingConfigurationResolution()
      return pluginParsingConfigurationResolution.resolveProblemLevelMapping(
        PluginParsingConfiguration(),
        JsonUrlProblemLevelRemappingManager.fromClassPathJson()
      )
    }
}
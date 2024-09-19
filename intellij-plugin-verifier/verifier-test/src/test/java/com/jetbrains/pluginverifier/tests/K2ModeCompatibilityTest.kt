package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.problems.UndeclaredKotlinK2CompatibilityMode
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.UndeclaredKotlinK2CompatibilityModeProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EXPECTED_MESSAGE =  "Invalid plugin descriptor 'plugin.xml'. " +
  "Plugin depends on the Kotlin plugin (org.jetbrains.kotlin) but does not declare " +
  "a compatibility mode in the <org.jetbrains.kotlin.supportsKotlinPluginMode> extension. " +
  "This feature is available for IntelliJ IDEA 2024.2.1 or later."

class K2ModeCompatibilityTest : BasePluginTest() {
  @Test
  fun `plugin does not declare K1-K2 compatibility for IDE 2024-2-1`() {
    val ide = buildIde("IU-242.21829.142")

    val ideaPlugin = ideaPlugin()
    val creationResult = buildPluginWithResult {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
              <depends>org.jetbrains.kotlin</depends>
            </idea-plugin>
          """
        }
      }
    }

    assertSuccess(creationResult) {
      assertContains<UndeclaredKotlinK2CompatibilityMode>(EXPECTED_MESSAGE)
      val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
      assertTrue(verificationResult is PluginVerificationResult.Verified)
      val verifiedResult = verificationResult as PluginVerificationResult.Verified
      with(verifiedResult.compatibilityProblems) {
        assertEquals(1, size)
        assertTrue(first() is UndeclaredKotlinK2CompatibilityModeProblem)
      }

      val structureProblems = verifiedResult.pluginStructureWarnings.map { it.problem }
      assertNoProblems(structureProblems)
    }
  }

  @Test
  fun `plugin does not declare K1-K2 compatibility but such IDE does not provide this feature`() {
    val ide = buildIde("IU-231.1")

    val ideaPlugin = ideaPlugin()
    val creationResult = buildPluginWithResult {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
              <depends>org.jetbrains.kotlin</depends>
            </idea-plugin>
          """
        }
      }
    }

    assertSuccess(creationResult) {
      assertContains<UndeclaredKotlinK2CompatibilityMode>(EXPECTED_MESSAGE)
      val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
      assertTrue(verificationResult is PluginVerificationResult.Verified)
      val verifiedResult = verificationResult as PluginVerificationResult.Verified
      assertEquals(0, verifiedResult.compatibilityProblems.size)
      val structureProblems = verifiedResult.pluginStructureWarnings.map { it.problem }
      with(structureProblems.filterIsInstance<UndeclaredKotlinK2CompatibilityMode>()) {
        assertEquals(1, size)
      }
    }
  }

}
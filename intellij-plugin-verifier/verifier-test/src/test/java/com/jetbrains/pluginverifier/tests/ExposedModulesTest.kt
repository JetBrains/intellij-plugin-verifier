/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ProhibitedModuleExposed
import com.jetbrains.plugin.structure.intellij.problems.remapping.JsonUrlProblemLevelRemappingManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.options.PluginParsingConfiguration
import com.jetbrains.pluginverifier.options.PluginParsingConfigurationResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposedModulesTest  : BasePluginTest() {
  @Test
  fun `plugin is a third-party vendor and exposes JetBrains modules`() {
    val ide = buildIde("IU-242.21829.142")

    val pluginId = "SomePlugin"
    val ideaPlugin = ideaPlugin(pluginId = pluginId, vendor = "SomeVendor")

    val creationResult = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
              <module value="com.intellij.modules.json"/>
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
      assertEquals(1, verifiedResult.pluginStructureWarnings.size)
      assertContains<ProhibitedModuleExposed>("Invalid plugin descriptor 'plugin.xml'. " +
        "Plugin declares a module with prohibited name: 'com.intellij.modules.json' has prefix 'com.intellij'. " +
        "Such modules cannot be declared by third party plugins.")
      assertEmpty("Plugin Warnings", warnings)
    }
  }

  @Test
  fun `plugin has a JetBrains vendor and exposes JetBrains modules`() {
    val ide = buildIde("IU-242.21829.142")

    val pluginId = "SomePlugin"
    val ideaPlugin = ideaPlugin(pluginId = pluginId, vendor = "JetBrains s.r.o.")

    val creationResult = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
              <module value="com.intellij.modules.json"/>
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

  private val problemResolver: PluginCreationResultResolver
    get() {
      val pluginParsingConfigurationResolution = PluginParsingConfigurationResolution()
      return pluginParsingConfigurationResolution.resolveProblemLevelMapping(
        PluginParsingConfiguration(),
        JsonUrlProblemLevelRemappingManager.fromClassPathJson()
      )
    }
}
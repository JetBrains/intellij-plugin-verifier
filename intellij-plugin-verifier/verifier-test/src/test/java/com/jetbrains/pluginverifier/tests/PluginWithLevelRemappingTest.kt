/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.IgnoredLevel
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.LevelRemappingPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionAndPluginVersionMismatch
import com.jetbrains.plugin.structure.intellij.problems.RemappedLevel
import com.jetbrains.plugin.structure.intellij.problems.TemplateWordInPluginName
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

class PluginWithLevelRemappingTest : BasePluginTest() {
  @Test
  fun `paid 3rd-party plugin has release-version that does not match plugin version and this is intentionally ignored`() {
    val paid3rdPartyPlugin = paidIdeaPlugin(vendor = "HornsAndHooves", pluginVersion = "2.1", releaseVersion = "20")
    val result = buildPluginWithResult(newProblemResolver()) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $paid3rdPartyPlugin
            </idea-plugin>
          """
        }
      }
    }
    assertSuccess(result)
  }

  @Test
  fun `internal paid plugin has release-version that does not match plugin version and this is intentionally ignored`() {
    val paidInternalIdeaPlugin = paidIdeaPlugin(vendor = "JetBrains", pluginVersion = "2.1", releaseVersion = "20")
    val result = buildPluginWithResult(newProblemResolver()) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $paidInternalIdeaPlugin
            </idea-plugin>
          """
        }
      }
    }
    assertSuccess(result)
  }

  @Test
  fun `3rd party plugin has a template word in the plugin name, but this is intentionally allowed`() {
    val paidInternalIdeaPlugin = paidIdeaPlugin(pluginName = "Paid Plugin", vendor = "HornsAndHooves")
    val result = buildPluginWithResult(newProblemResolver()) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $paidInternalIdeaPlugin
            </idea-plugin>
          """
        }
      }
    }
    assertSuccess(result) {
      assertTrue(warnings.isEmpty())
    }
  }

  /**
   * Create a problem resolver with the following rules:
   * * Ignored in JetBrains plugins:
   *      * ForbiddenPluginIdPrefix
   *      * ReleaseVersionAndPluginVersionMismatch
   *      * TemplateWordInPluginName
   * * Ignored in 3rd-party plugins:
   *      * ReleaseVersionAndPluginVersionMismatch
   *      * TemplateWordInPluginName
   */
  private fun newProblemResolver(): PluginCreationResultResolver {
    val defaultProblemResolver = IntelliJPluginCreationResultResolver()

    val jetBrainsPluginProblemRemapping: Map<KClass<*>, RemappedLevel> = mapOf(
      ReleaseVersionAndPluginVersionMismatch::class to IgnoredLevel,
      ForbiddenPluginIdPrefix::class to IgnoredLevel,
      TemplateWordInPluginName::class to IgnoredLevel,
    )
    val thirdPartyPluginProblemRemapping: Map<KClass<*>, RemappedLevel> = mapOf(
      ReleaseVersionAndPluginVersionMismatch::class to IgnoredLevel,
      TemplateWordInPluginName::class to IgnoredLevel
    )

    val thirdPartyProblemResolver = LevelRemappingPluginCreationResultResolver(defaultProblemResolver, thirdPartyPluginProblemRemapping, unwrapRemappedProblems = true)
    return JetBrainsPluginCreationResultResolver(thirdPartyProblemResolver, jetBrainsPluginProblemRemapping)
  }

  private fun paidIdeaPlugin(pluginId: String = "someid",
                             pluginName: String = "someName",
                             pluginVersion: String = "1",
                             vendor: String = "vendor",
                             sinceBuild: String = "131.1",
                             untilBuild: String = "231.1",
                             description: String = "this description is looooooooooong enough",
                             releaseVersion: String = "20211") =
    ideaPlugin(pluginId, pluginName, pluginVersion, vendor, sinceBuild, untilBuild, description) +
      """
        <product-descriptor code="PTESTPLUGIN" release-date="20210818" release-version="$releaseVersion"/>
      """.trimIndent()
}
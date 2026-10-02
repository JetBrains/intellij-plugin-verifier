/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import org.junit.Assert.assertEquals
import org.junit.Test

class ModulesAndDependenciesTest : DependenciesTestBase() {
  @Test
  fun `mandatory V2 plugin dependency in implicitly optional content module is optional`() {
    assertContentModuleDependencyOptionality(loadingRule = null, isOptional = true)
  }

  @Test
  fun `mandatory V2 plugin dependency in optional content module is optional`() {
    assertContentModuleDependencyOptionality(loadingRule = ModuleLoadingRule.OPTIONAL, isOptional = true)
  }

  @Test
  fun `mandatory V2 plugin dependency in required content module is mandatory`() {
    assertContentModuleDependencyOptionality(loadingRule = ModuleLoadingRule.REQUIRED, isOptional = false)
  }

  private fun assertContentModuleDependencyOptionality(loadingRule: ModuleLoadingRule?, isOptional: Boolean) {
    val loadingAttribute = loadingRule?.let { "loading=\"${it.id}\"" }.orEmpty()
    val plugin = buildPlugin(
      """
        <idea-plugin>
          <id>com.example.plugin</id>
          <name>Example plugin</name>
          <version>1.0</version>
          <vendor>JetBrains</vendor>
          <description>Example plugin used to verify content module dependency optionality.</description>
          <change-notes>Example plugin used to verify content module dependency optionality.</change-notes>
          <idea-version since-build="241.0"/>
          <content>
            <module name="com.example.plugin.content" $loadingAttribute/>
          </content>
        </idea-plugin>
      """.trimIndent(),
      listOf(
        ModuleDescriptorSource(
          archive = "content.jar",
          module = "com.example.plugin.content",
          xml = """
            <idea-plugin>
              <dependencies>
                <plugin id="dependency.plugin"/>
              </dependencies>
            </idea-plugin>
          """.trimIndent(),
        ),
      ),
    )

    val dependency = plugin.dependencies.filterIsInstance<PluginV2Dependency>().single { it.id == "dependency.plugin" }
    assertEquals(isOptional, dependency.isOptional)
  }
}
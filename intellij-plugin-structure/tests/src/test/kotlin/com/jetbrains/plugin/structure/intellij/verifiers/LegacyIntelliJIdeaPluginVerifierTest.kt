/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.problems.NoDependencies
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyIntelliJIdeaPluginVerifierTest : BaseExtensionPointTest<LegacyIntelliJIdeaPluginVerifier>(LegacyIntelliJIdeaPluginVerifier()) {
  @Test
  fun `no v1 dependencies`() {
    val plugin = MockIdePlugin()
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)

    assertEquals(1, problems.size)
    with(problems.first()) {
      assertTrue(this is NoDependencies)
    }
  }

  @Test
  fun `only one dependency on com_intellij_modules_platform`() {
    val plugin = MockIdePlugin(dependencies = listOf(PluginV1Dependency.Mandatory("com.intellij.modules.platform")))
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)

    assertEquals(0, problems.size)
  }

  @Test
  fun `only one dependency on a plugin`() {
    val plugin = MockIdePlugin(dependencies = listOf(PluginV1Dependency.Mandatory("JavaScript")))
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)

    assertEquals(1, problems.size)
    with(problems.first()) {
      assertTrue(this is NoModuleDependencies)
    }
  }

  @Test
  fun `dependency just on com_intellij_modules_lang`() {
    val plugin = MockIdePlugin(dependencies = listOf(PluginV1Dependency.Mandatory("com.intellij.modules.lang")))
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)

    assertEquals(0, problems.size)
  }

  @Test
  fun `dependency just on com_intellij_modules_ultimate`() {
    val plugin = MockIdePlugin(dependencies = listOf(PluginV1Dependency.Mandatory("com.intellij.modules.ultimate")))
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)

    assertEquals(0, problems.size)
  }

  @Test
  fun `plugin with a single v2 dependency as a ModuleV2Dependency`() {
    val plugin = MockIdePlugin(
      pluginId = "com.intellij.classic.ui",
      incompatibleWith = listOf("com.intellij.jetbrains.client"),
      dependencies = listOf(ModuleV2Dependency("intellij.platform.monolith")),
      contentModuleDependencies = listOf(ContentModuleDependency("intellij.platform.monolith", "jetbrains"))
    )
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)
    assertEquals(0, problems.size)
  }

  @Test
  fun `plugin with a single v2 dependency as a plugin v2 dependency`() {
    val plugin = MockIdePlugin(
      pluginId = "com.intellij.classic.ui",
      incompatibleWith = listOf("com.intellij.jetbrains.client"),
      // using legacy class for dependency
      dependencies = listOf(PluginV2Dependency ("intellij.platform.monolith")),
      contentModuleDependencies = listOf(ContentModuleDependency("intellij.platform.monolith", "jetbrains"))
    )
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)
    assertEquals(0, problems.size)
  }

  @Test
  fun `plugin without any dependencies, but with a content module is not legacy`() {
    val contentModuleMetadata = InlineModule("someContentModule", "someNamespace", "someNamespace", ModuleLoadingRule.REQUIRED, "<idea-plugin />")
    val contentModule = MockIdePlugin()
    val contentModuleDescriptor = ModuleDescriptor(contentModule, contentModuleMetadata)
    val plugin = MockIdePlugin(
      pluginId = "somePlugin",
      dependencies = emptyList(),
      pluginMainModuleDependencies = emptyList(),
      contentModuleDependencies = emptyList(),
      contentModules = listOf(contentModuleMetadata),
      modulesDescriptors = listOf(contentModuleDescriptor)
    )
    verifier.verify(plugin, PLUGIN_XML, problemRegistrar)
    assertEquals(0, problems.size)
  }
}
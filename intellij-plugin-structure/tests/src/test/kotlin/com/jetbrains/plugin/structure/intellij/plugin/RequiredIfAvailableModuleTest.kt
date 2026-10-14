/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.mocks.SimplePluginCreatorResultResolver
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Verifies that the `required-if-available` attribute of a content module is exposed as-is,
 * without affecting the loading rule derived from the `loading` attribute.
 */
class RequiredIfAvailableModuleTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `inline module with required-if-available is optional and exposes the attribute`() {
    val module = createPlugin().getContentModule("intellij.example.inline")

    assertTrue(module is Module.InlineModule)
    assertEquals("intellij.platform.backend", module.requiredIfAvailable)
    assertEquals(ModuleLoadingRule.OPTIONAL, module.loadingRule)
  }

  @Test
  fun `file based module with required-if-available is optional and exposes the attribute`() {
    val plugin = createPlugin()
    val module = plugin.getContentModule("intellij.example.file")

    assertTrue(module is Module.FileBasedModule)
    assertEquals("intellij.platform.backend", module.requiredIfAvailable)
    assertEquals(ModuleLoadingRule.OPTIONAL, module.loadingRule)

    val moduleDescriptor = plugin.modulesDescriptors.single { it.name == "intellij.example.file" }
    assertEquals("intellij.platform.backend", moduleDescriptor.moduleDefinition.requiredIfAvailable)
  }

  @Test
  fun `module with both loading and required-if-available exposes both values`() {
    val module = createPlugin().getContentModule("intellij.example.required")

    assertEquals("intellij.platform.frontend", module.requiredIfAvailable)
    assertEquals(ModuleLoadingRule.REQUIRED, module.loadingRule)
  }

  @Test
  fun `module without required-if-available has no value`() {
    val module = createPlugin().getContentModule("intellij.example.plain")

    assertNull(module.requiredIfAvailable)
    assertEquals(ModuleLoadingRule.OPTIONAL, module.loadingRule)
  }

  private fun IdePlugin.getContentModule(name: String): Module = contentModules.single { it.name == name }

  private fun createPlugin(): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath()) {
      dir("META-INF") {
        file("plugin.xml", pluginXmlContent)
      }
      file("intellij.example.file.xml", "<idea-plugin />")
    }
    val creationResult = IdePluginManager.createManager()
      .createPlugin(pluginFile, validateDescriptor = true, problemResolver = SimplePluginCreatorResultResolver())
    assertTrue("Expected a successfully created plugin but got $creationResult", creationResult is PluginCreationSuccess)
    return (creationResult as PluginCreationSuccess).plugin
  }

  @Language("XML")
  private val pluginXmlContent = """
      <idea-plugin>
          <id>com.example.plugin</id>
          <vendor>JetBrains</vendor>
          <content namespace="jetbrains">
              <module name="intellij.example.inline" required-if-available="intellij.platform.backend"><![CDATA[<idea-plugin />]]></module>
              <module name="intellij.example.file" required-if-available="intellij.platform.backend"/>
              <module name="intellij.example.required" loading="required" required-if-available="intellij.platform.frontend"><![CDATA[<idea-plugin />]]></module>
              <module name="intellij.example.plain"><![CDATA[<idea-plugin />]]></module>
          </content>
      </idea-plugin>
    """.trimIndent()
}

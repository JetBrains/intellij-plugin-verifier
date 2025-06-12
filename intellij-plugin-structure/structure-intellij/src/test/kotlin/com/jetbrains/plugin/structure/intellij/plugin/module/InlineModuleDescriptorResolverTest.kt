/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.intellij.plugin.IdePluginImpl
import com.jetbrains.plugin.structure.intellij.plugin.InlineDeclaredModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.loaders.ModuleFromDescriptorLoader
import org.junit.Assert.assertEquals
import org.junit.Test

class InlineModuleDescriptorResolverTest {
  /**
   * <idea-plugin package="com.intellij.thymeleaf">
   *   <id>com.intellij.thymeleaf</id>
   *   <content>
   *     <module name="intellij.thymeleaf/spring-el"><![CDATA[
   *       <idea-plugin package="com.intellij.thymeleaf.spring">
   *         <dependencies>
   *           <module name="intellij.spring.el" />
   */
  @Test
  fun `inline content module has dependency on a module`() {
    val thymeleafSpringElPlugin = IdePluginImpl().apply {
      hasPackagePrefix = true
      dependencies += ModuleV2Dependency("intellij.spring.el")
    }

    val thymeleafSpringElPluginXml = """
      <idea-plugin package="com.intellij.thymeleaf.spring">
        <dependencies>
          <module name="intellij.spring.el" />
        </dependencies>
      </idea-plugin>              
      """.trimIndent()

    val thymeleafSpringElInlineModule = InlineModule(
      "intellij.thymeleaf/spring-el",
      loadingRule = ModuleLoadingRule.OPTIONAL,
      thymeleafSpringElPluginXml
    )

    val thymeleafPlugin = IdePluginImpl().apply {
      pluginId = "com.intellij.thymeleaf"
      contentModules += thymeleafSpringElInlineModule
    }

    val loader = ModuleFromDescriptorLoader()
    val resolver = InlineModuleDescriptorResolver(loader)
    val dependencies = resolver.getDependencies(thymeleafPlugin, thymeleafSpringElPlugin, thymeleafSpringElInlineModule)
    with(dependencies) {
      assertEquals(1, size)
      assertEquals(
        InlineDeclaredModuleV2Dependency.onModule(
          "intellij.spring.el",
          ModuleLoadingRule.OPTIONAL,
          thymeleafPlugin,
          thymeleafSpringElInlineModule
        ), single()
      )
    }
  }

  /**
   * See:
   * ```
   * <idea-plugin
   *   <id>org.toml.lang</id>
   *   <content>
   *     <module name="intellij.toml.json"><![CDATA[
   *       <idea-plugin>
   *         <dependencies>
   *           <plugin id="com.intellij.modules.json" />
   * ```
   */
  @Test
  fun `inline content module has dependency on a plugin`() {
    val intellijTomJsonPlugin = IdePluginImpl().apply {
      dependencies += PluginV2Dependency("com.intellij.modules.json")
    }

    val intellijTomJsonPluginXml = """
      <idea-plugin>
        <dependencies>
          <plugin id="com.intellij.modules.json" />
        </dependencies>
      </idea-plugin>              
    """.trimIndent()


    val intellijTomJsonInlineModule = InlineModule(
      name = "intellij.toml.json",
      loadingRule = ModuleLoadingRule.OPTIONAL,
      textContent = intellijTomJsonPluginXml
    )

    val tomlPlugin = IdePluginImpl().apply {
      pluginId = "org.toml.lang"
      contentModules += intellijTomJsonInlineModule
    }

    val loader = ModuleFromDescriptorLoader()
    val resolver = InlineModuleDescriptorResolver(loader)
    val dependencies = resolver.getDependencies(tomlPlugin, intellijTomJsonPlugin, intellijTomJsonInlineModule)
    with(dependencies) {
      assertEquals(1, size)
      assertEquals(
        InlineDeclaredModuleV2Dependency.onPlugin(
          "com.intellij.modules.json",
          ModuleLoadingRule.OPTIONAL,
          tomlPlugin,
          intellijTomJsonInlineModule
        ), single()
      )
    }
  }
}
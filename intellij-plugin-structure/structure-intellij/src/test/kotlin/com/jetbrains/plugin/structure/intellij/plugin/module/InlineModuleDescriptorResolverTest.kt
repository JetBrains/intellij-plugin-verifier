/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildDirectory
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.loaders.ModuleFromDescriptorLoader
import com.jetbrains.plugin.structure.intellij.resources.DefaultResourceResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InlineModuleDescriptorResolverTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

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
      addContentModuleDependency(ContentModuleDependency("intellij.spring.el", "jetbrains"))
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
      namespace = null,
      actualNamespace = "jetbrains",
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
      addPluginMainModuleDependency(PluginMainModuleDependency("com.intellij.modules.json"))
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
      namespace = null,
      actualNamespace = "jetbrains",
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

  @Test
  fun `parsed inline modules retain repeated declarations before main dependency filtering`() {
    val loadingRules = listOf("required" to "required", "optional" to "optional", "default" to null)
    val moduleXml = """
      <idea-plugin>
        <depends optional="true">shared.legacy</depends>
        <dependencies>
          <plugin id="shared.plugin"/>
          <module name="shared.module"/>
          <plugin id="module.only.plugin"/>
          <module name="module.only.module"/>
        </dependencies>
      </idea-plugin>
    """.trimIndent()
    val moduleReferences = loadingRules.joinToString("\n") { (name, loading) ->
      val loadingAttribute = loading?.let { " loading=\"$it\"" }.orEmpty()
      "<module name=\"example.$name\"$loadingAttribute><![CDATA[$moduleXml]]></module>"
    }
    val pluginPath = buildDirectory(temporaryFolder.newFolder("repeated-declarations").toPath()) {
      dir("META-INF") {
        file("plugin.xml", """
          <idea-plugin>
            <id>com.example.plugin</id>
            <name>Example</name>
            <version>1.0</version>
            <vendor>JetBrains</vendor>
            <description>A plugin with repeated inline module dependencies.</description>
            <idea-version since-build="241.0"/>
            <depends>shared.legacy</depends>
            <dependencies>
              <plugin id="shared.plugin"/>
              <module name="shared.module"/>
            </dependencies>
            <content>
              $moduleReferences
            </content>
          </idea-plugin>
        """.trimIndent())
      }
    }
    val manager = IdePluginManager.createManager(DefaultResourceResolver, temporaryFolder.newFolder().toPath())
    val creationResult = manager.createPlugin(pluginPath, false)
    assertTrue("Expected a successfully created plugin but got $creationResult", creationResult is PluginCreationSuccess)
    val plugin = (creationResult as PluginCreationSuccess).plugin

    assertEquals(3, plugin.modulesDescriptors.size)
    loadingRules.forEach { (name, loading) ->
      val descriptor = plugin.modulesDescriptors.single { it.name == "example.$name" }
      assertTrue(descriptor is InlineModuleDescriptor)
      val moduleReference = descriptor.moduleDefinition as InlineModule
      assertEquals(loading != "required", !moduleReference.loadingRule.required)
      val pluginDependency = InlineDeclaredModuleV2Dependency.onPlugin(
        "module.only.plugin", moduleReference.loadingRule, plugin, moduleReference
      )
      val moduleDependency = InlineDeclaredModuleV2Dependency.onModule(
        "module.only.module", moduleReference.loadingRule, plugin, moduleReference
      )
      assertEquals(setOf(pluginDependency, moduleDependency), descriptor.resolvedDependencies.toSet())
      assertEquals(2, descriptor.resolvedDependencies.size)
      assertEquals(
        setOf(
          PluginV1Dependency.Optional("shared.legacy"),
          InlineDeclaredModuleV2Dependency.onPlugin("shared.plugin", moduleReference.loadingRule, plugin, moduleReference),
          InlineDeclaredModuleV2Dependency.onModule("shared.module", moduleReference.loadingRule, plugin, moduleReference),
          pluginDependency,
          moduleDependency
        ),
        descriptor.declaredDependencies.toSet()
      )
      assertEquals(5, descriptor.declaredDependencies.size)
      descriptor.declaredDependencies.filterIsInstance<InlineDeclaredModuleV2Dependency>().forEach { dependency ->
        assertEquals(loading != "required", dependency.isOptional)
        assertEquals("com.example.plugin", dependency.contentModuleOwnerId)
        assertEquals("example.$name", dependency.dependerContentModuleId)
      }
    }
    val requiredModule = plugin.modulesDescriptors.single { it.name == "example.required" }.moduleDefinition as InlineModule
    assertEquals(
      setOf(
        PluginV1Dependency.Mandatory("shared.legacy"),
        PluginV2Dependency("shared.plugin"),
        ModuleV2Dependency("shared.module"),
        InlineDeclaredModuleV2Dependency.onPlugin("module.only.plugin", ModuleLoadingRule.REQUIRED, plugin, requiredModule),
        InlineDeclaredModuleV2Dependency.onModule("module.only.module", ModuleLoadingRule.REQUIRED, plugin, requiredModule)
      ),
      plugin.dependencies.toSet()
    )
    assertEquals(5, plugin.dependencies.size)
  }

  @Test
  fun `module descriptor declarations default to resolved dependencies`() {
    val module = IdePluginImpl()
    val inlineModule = InlineModule("example.inline", null, "jetbrains", ModuleLoadingRule.OPTIONAL, "<idea-plugin/>")
    val fileModule = Module.FileBasedModule("example.file", null, "jetbrains", ModuleLoadingRule.OPTIONAL, "example.file.xml")
    val dependencies = listOf(PluginV2Dependency("example.plugin", isOptional = true), ModuleV2Dependency("example.module"))
    val descriptors = listOf(
      ModuleDescriptor.of(module, inlineModule, dependencies),
      ModuleDescriptor.of(module, fileModule, dependencies),
      InlineModuleDescriptor(module, inlineModule, dependencies),
      FileBasedModuleDescriptor(module, fileModule, dependencies)
    )

    descriptors.forEach { descriptor ->
      assertEquals(dependencies, descriptor.resolvedDependencies)
      assertEquals(dependencies, descriptor.declaredDependencies)
    }
  }
}
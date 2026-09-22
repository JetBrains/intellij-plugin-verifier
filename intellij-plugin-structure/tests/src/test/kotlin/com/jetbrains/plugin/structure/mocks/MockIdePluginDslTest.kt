package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import org.junit.Assert.assertEquals
import org.junit.Test

class MockIdePluginDslTest {
  @Test
  fun `creates plugin with id`() {
    val plugin = idePlugin("com.example.somePlugin")

    assertEquals("com.example.somePlugin", plugin.pluginId)
    assertEquals("com.example.somePlugin", plugin.pluginName)
  }

  @Test
  fun `creates plugin with all dependency types`() {
    val plugin = idePlugin("com.example.somePlugin") {
      depends("com.jetbrains.platform")
      depends("com.jetbrains.platform.second")
      optionalDepends("com.jetbrains.kotlin")
      optionalDepends("com.jetbrains.kotlin.second")
      pluginDependency("com.jetbrains.css")
      pluginDependency("com.jetbrains.js")
      moduleDependency("intellij.css")
      moduleDependency("intellij.kt")
    }

    assertEquals(
      listOf("com.jetbrains.platform", "com.jetbrains.platform.second"),
      plugin.dependsList.filterNot { it.isOptional }.map { it.pluginId }
    )
    assertEquals(
      listOf("com.jetbrains.kotlin", "com.jetbrains.kotlin.second"),
      plugin.dependsList.filter { it.isOptional }.map { it.pluginId }
    )
    assertEquals(
      listOf("com.jetbrains.css", "com.jetbrains.js"),
      plugin.pluginMainModuleDependencies.map { it.pluginId }
    )
    assertEquals(
      listOf("intellij.css", "intellij.kt"),
      plugin.contentModuleDependencies.map { it.moduleName }
    )
    assertEquals(
      listOf("jetbrains", "jetbrains"),
      plugin.contentModuleDependencies.map { it.namespace }
    )
  }

  @Test
  @Suppress("DEPRECATION")
  fun `exposes dsl dependencies through mixed dependencies view`() {
    val plugin = idePlugin("com.example.somePlugin") {
      depends("com.jetbrains.platform")
      optionalDepends("com.jetbrains.kotlin")
      pluginDependency("com.jetbrains.css")
      moduleDependency("intellij.css")
    }

    assertEquals(
      listOf(
        PluginV1Dependency.Mandatory("com.jetbrains.platform"),
        PluginV1Dependency.Optional("com.jetbrains.kotlin"),
        ModuleV2Dependency("intellij.css"),
        PluginV2Dependency("com.jetbrains.css"),
      ),
      plugin.dependencies
    )
  }

  @Test
  fun `allows custom namespace for module dependencies`() {
    val plugin = idePlugin("com.example.somePlugin") {
      moduleDependency("intellij.css", namespace = "custom")
    }

    val dependency = plugin.contentModuleDependencies.single()
    assertEquals("intellij.css", dependency.moduleName)
    assertEquals("custom", dependency.namespace)
  }
}
package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.dependencies.CorePluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy.LegacyPluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.idePlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

private const val CORE_PLUGIN_ID = "com.intellij"

class CompositeDependenciesModifierTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path
  private lateinit var corePlugin: MockIdePlugin
  private lateinit var ide: MockIde

  @Before
  fun setUp() {
    ideRoot = temporaryFolder.newFolder("idea").toPath()
    corePlugin = MockIdePlugin(
      pluginId = CORE_PLUGIN_ID,
      pluginAliases = setOf("com.intellij.modules.all", "com.intellij.modules.platform")
    )
    ide = MockIde(IdeVersion.createIdeVersion("IU-261.1000"), ideRoot, listOf(corePlugin))
  }

  @Test
  fun `composite modifier applies modifiers in sequence`() {
    val javaPlugin = MockIdePlugin(
      pluginName = "Java",
      pluginId = "com.intellij.java",
      pluginAliases = setOf("com.intellij.modules.java")
    )
    val bundledPlugins = listOf(corePlugin, javaPlugin)
    val ide = MockIde(IdeVersion.createIdeVersion("IU-261.1000"), ideRoot, bundledPlugins)

    // Legacy plugin has no module dependencies
    val legacyPlugin = idePlugin("com.example.Legacy")

    val legacyPluginVerifier = LegacyIntelliJIdeaPluginVerifier()
    val compositeModifier = CompositeDependenciesModifier(
      CorePluginDependencyContributor(ide),
      LegacyPluginDependencyContributor(ide, legacyPluginVerifier)
    )

    // Test the composite modifier directly
    val modifiedDependencies = compositeModifier.apply(legacyPlugin, ide)

    // Should have core plugin (from CorePluginDependencyContributor)
    assertTrue(
      "Should contain core plugin dependency",
      modifiedDependencies.any { it.dependency.id == CORE_PLUGIN_ID }
    )
    assertEquals(DependencyModificationReason.IDE, modifiedDependencies.reasonOf(CORE_PLUGIN_ID))
    // Should have Java module (from LegacyPluginDependencyContributor for legacy plugins)
    assertTrue(
      "Should contain Java module dependency (from legacy contributor)",
      modifiedDependencies.any { it.dependency.id == "com.intellij.modules.java" }
    )
    assertEquals(DependencyModificationReason.IDE, modifiedDependencies.reasonOf("com.intellij.modules.java"))
  }

  @Test
  fun `composite modifier with empty list returns original dependencies`() {
    val plugin = idePlugin("com.example.plugin") {
      depends("some.dependency")
    }

    val compositeModifier = CompositeDependenciesModifier(emptyList())
    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(1, modifiedDependencies.size)
    assertEquals("some.dependency", modifiedDependencies.first().dependency.id)
    assertEquals(DependencyModificationReason.PLUGIN, modifiedDependencies.first().reason)
  }

  @Test
  fun `pass-through modifier infers reasons from merged dependencies`() {
    val plugin = idePlugin("com.example.plugin") {
      depends("com.example.v1")
      pluginDependency("com.example.v2")
      moduleDependency("com.example.content")
    }

    val modifiedDependencies = PassThruDependenciesModifier.apply(plugin, ide)

    assertEquals(DependencyModificationReason.PLUGIN, modifiedDependencies.reasonOf("com.example.v1"))
    assertEquals(DependencyModificationReason.PLUGIN, modifiedDependencies.reasonOf("com.example.v2"))
    assertEquals(DependencyModificationReason.CONTENT_MODULE, modifiedDependencies.reasonOf("com.example.content"))
  }

  @Test
  fun `composite modifier uses highest priority reason for duplicate dependency ids`() {
    val sharedDependencyId = "com.example.shared"
    val plugin = MockIdePlugin(pluginId = "com.example.plugin")
    val compositeModifier = CompositeDependenciesModifier(
      { _, _ ->
        listOf(
          DependencyModification(
            PluginV1Dependency.Mandatory(sharedDependencyId), DependencyModificationReason.IDE
          )
        )
      },
      { _, _ ->
        listOf(
          DependencyModification(
            PluginV1Dependency.Mandatory(sharedDependencyId), DependencyModificationReason.PLUGIN
          )
        )
      },
      { _, _ ->
        listOf(
          DependencyModification(
            PluginV1Dependency.Mandatory(sharedDependencyId), DependencyModificationReason.OTHER
          )
        )
      },
      { _, _ ->
        listOf(
          DependencyModification(
            ModuleV2Dependency(sharedDependencyId), DependencyModificationReason.CONTENT_MODULE
          )
        )
      }
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(1, modifiedDependencies.size)
    assertEquals(sharedDependencyId, modifiedDependencies.first().dependency.id)
    assertEquals(DependencyModificationReason.CONTENT_MODULE, modifiedDependencies.first().reason)
  }

  private fun List<DependencyModification>.reasonOf(id: String) = first { it.dependency.id == id }.reason
}
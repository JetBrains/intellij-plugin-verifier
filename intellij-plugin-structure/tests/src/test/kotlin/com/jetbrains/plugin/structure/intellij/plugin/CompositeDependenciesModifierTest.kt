package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.dependencies.CorePluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy.LegacyPluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
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
    val legacyPlugin = MockIdePlugin(
      pluginId = "com.example.Legacy",
      dependencies = emptyList()
    )

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
      modifiedDependencies.any { it.id == CORE_PLUGIN_ID }
    )
    // Should have Java module (from LegacyPluginDependencyContributor for legacy plugins)
    assertTrue(
      "Should contain Java module dependency (from legacy contributor)",
      modifiedDependencies.any { it.id == "com.intellij.modules.java" }
    )
  }

  @Test
  fun `composite modifier with empty list returns original dependencies`() {
    val plugin = MockIdePlugin(
      pluginId = "com.example.plugin",
      dependencies = listOf(PluginV1Dependency.Mandatory("some.dependency"))
    )

    val compositeModifier = CompositeDependenciesModifier(emptyList())
    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(1, modifiedDependencies.size)
    assertEquals("some.dependency", modifiedDependencies.first().id)
  }
}
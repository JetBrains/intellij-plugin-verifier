package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.IdPrefixIdeModulePredicate.Companion.HAS_COM_INTELLIJ_MODULE_PREFIX
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

class CorePluginDependencyContributorTest {
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
  fun `core plugin dependency is added for v2 plugin without explicit core dependency`() {
    val v2Plugin = MockIdePlugin(
      pluginId = "com.example.v2plugin",
      dependencies = listOf(PluginV1Dependency.Mandatory("com.intellij.modules.platform"))
    )

    val contributor = CorePluginDependencyContributor(ide)
    val modifiedDependencies = contributor.apply(v2Plugin, ide)

    assertEquals(2, modifiedDependencies.size)
    assertTrue(
      "Should contain original dependency",
      modifiedDependencies.any { it.id == "com.intellij.modules.platform" }
    )
    assertTrue(
      "Should contain core plugin dependency",
      modifiedDependencies.any { it.id == CORE_PLUGIN_ID }
    )
  }

  @Test
  fun `core plugin dependency is not duplicated if already present`() {
    val pluginWithCoreDependency = MockIdePlugin(
      pluginId = "com.example.plugin",
      dependencies = listOf(PluginV1Dependency.Mandatory(CORE_PLUGIN_ID))
    )

    val contributor = CorePluginDependencyContributor(ide)
    val modifiedDependencies = contributor.apply(pluginWithCoreDependency, ide)

    assertEquals(1, modifiedDependencies.size)
    assertEquals(CORE_PLUGIN_ID, modifiedDependencies.first().id)
  }

  @Test
  fun `core plugin does not add dependency on itself`() {
    val contributor = CorePluginDependencyContributor(ide)
    val modifiedDependencies = contributor.apply(corePlugin, ide)

    assertTrue(
      "Core plugin should not have dependency on itself",
      modifiedDependencies.none { it.id == CORE_PLUGIN_ID }
    )
  }

  @Test
  fun `core plugin dependency is added even for plugins with no dependencies`() {
    val pluginWithNoDeps = MockIdePlugin(
      pluginId = "com.example.nodeps",
      dependencies = emptyList()
    )

    val contributor = CorePluginDependencyContributor(ide)
    val modifiedDependencies = contributor.apply(pluginWithNoDeps, ide)

    assertEquals(1, modifiedDependencies.size)
    assertEquals(CORE_PLUGIN_ID, modifiedDependencies.first().id)
  }

  @Test
  fun `dependency tree includes core plugin classes for v2 plugins`() {
    val v2Plugin = MockIdePlugin(
      pluginId = "com.example.v2plugin",
      dependencies = listOf(PluginV1Dependency.Mandatory("com.intellij.modules.platform"))
    )

    val contributor = CorePluginDependencyContributor(ide)
    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)

    val transitiveDependencies =
      dependencyTree.getTransitiveDependencies(v2Plugin, dependenciesModifier = contributor)

    // Core plugin should be in transitive dependencies
    assertTrue(
      "Core plugin should be in transitive dependencies",
      transitiveDependencies.any { it.id == CORE_PLUGIN_ID }
    )
  }
}
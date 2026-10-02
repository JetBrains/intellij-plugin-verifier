/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.DefaultDependencyContributor
import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.Module
import com.jetbrains.plugin.structure.intellij.plugin.ModuleDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ModuleLoadingRule
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTree
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTreeResolution
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.IdPrefixIdeModulePredicate.Companion.HAS_COM_INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphProvider
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.ResolvedDependencyNode
import com.jetbrains.pluginverifier.dependencies.ResolvedPluginDependency
import com.jetbrains.pluginverifier.dependencies.toResolved
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DependenciesGraphProviderTest {
  @Test
  fun `module declarations repeated in main descriptor retain their reporting edges`() {
    val fixture = modularDependenciesFixture(repeatMainDependencies = true)

    assertEquals(setOf("com.intellij.modules.platform", "bundled"), fixture.plugin.reconstructDependencies().map { it.id }.toSet())
    assertTrue(fixture.plugin.modulesDescriptors.first().resolvedDependencies.isEmpty())
    assertEquals(listOf("owner.core"), fixture.plugin.modulesDescriptors.last().resolvedDependencies.map { it.id })
    assertModularGraph(fixture, repeatMainDependencies = true)
  }

  @Test
  fun `module-only dependencies retain ownership vertices absent from flattened dependencies`() {
    val fixture = modularDependenciesFixture(repeatMainDependencies = false)

    assertTrue(fixture.plugin.reconstructDependencies().isEmpty())
    assertModularGraph(fixture, repeatMainDependencies = false)
  }

  @Test
  fun `module identity includes its owner before and after resolved conversion`() {
    val root = DependencyNode.PluginDependency(MockIdePlugin(pluginId = "root", pluginVersion = "1.0"))
    val firstOwner = MockIdePlugin(pluginId = "first", pluginVersion = "1.0")
    val secondOwner = MockIdePlugin(pluginId = "second", pluginVersion = "1.0")
    val first = DependencyNode.ModuleDependency(firstOwner, "shared")
    val second = DependencyNode.ModuleDependency(secondOwner, "shared")
    val firstAgain = DependencyNode.ModuleDependency(firstOwner, "shared")

    assertEquals("shared", first.id)
    assertEquals("1.0", first.version)
    assertEquals("first/shared:1.0", first.toString())
    assertEquals(first, firstAgain)
    assertEquals(first.hashCode(), firstAgain.hashCode())
    assertNotEquals(first, second)
    assertNotEquals(first, DependencyNode.ModuleDependency(firstOwner, "other"))
    assertNotEquals(first, DependencyNode.PluginDependency(firstOwner))
    assertEquals(2, setOf(first, firstAgain, second).size)

    val dependency = PluginDependencyImpl("shared", false, true)
    val graph = DependenciesGraph(
      root,
      setOf(root, first, second),
      setOf(DependencyEdge(root, first, dependency), DependencyEdge(root, second, dependency)),
      mapOf(
        first to setOf(MissingDependency(PluginDependencyImpl("missing.first", false, false), "Unavailable")),
        second to setOf(MissingDependency(PluginDependencyImpl("missing.second", false, false), "Unavailable"))
      )
    )
    val resolved = graph.toResolved()
    val resolvedFirst = ResolvedDependencyNode("shared", "1.0", moduleOwnerId = "first")
    val resolvedSecond = ResolvedDependencyNode("shared", "1.0", moduleOwnerId = "second")

    assertNotEquals(resolvedFirst, resolvedSecond)
    assertEquals(setOf(resolved.verifiedPlugin, resolvedFirst, resolvedSecond), resolved.vertices)
    assertEquals(setOf(resolvedFirst, resolvedSecond), resolved.edges.map { it.to }.toSet())
    assertEquals(2, resolved.edges.size)
    assertEquals("first/shared:1.0", resolvedFirst.toString())
    assertEquals("second/shared:1.0", resolvedSecond.toStringWithAliases())
    assertEquals(setOf("missing.first"), resolved.missingDependencies.getValue(resolvedFirst).map { it.dependency.id }.toSet())
    assertEquals(setOf("missing.second"), resolved.missingDependencies.getValue(resolvedSecond).map { it.dependency.id }.toSet())
    assertNull(ResolvedDependencyNode("shared", "1.0").moduleOwnerId)
    assertEquals("shared:1.0", ResolvedDependencyNode("shared", "1.0").toString())
  }

  @Test
  fun `legacy aliases and product modules remain plugin nodes`() {
    val graph = legacyDependenciesGraph()

    assertTrue(graph.vertices.all { it is DependencyNode.PluginDependency })
    assertEquals(setOf("legacy", "com.intellij", "com.intellij.modules.product"), graph.vertices.map { it.id }.toSet())
    val platform = graph.vertices.single { it.id == "com.intellij" } as DependencyNode.PluginDependency
    val product = graph.vertices.single { it.id == "com.intellij.modules.product" } as DependencyNode.PluginDependency
    assertEquals(setOf("com.intellij.modules.platform"), platform.aliases)
    assertEquals(setOf("com.intellij.modules.product"), product.aliases)
    assertTrue(product.plugin is IdeModule)
    assertEquals(setOf(platform, product), graph.getEdgesFrom(graph.verifiedPlugin).map { it.to }.toSet())

    val resolved = graph.toResolved()
    assertTrue(resolved.vertices.all { it.moduleOwnerId == null })
    assertEquals(platform.aliases, resolved.vertices.single { it.id == platform.id }.aliases)
    assertEquals(product.aliases, resolved.vertices.single { it.id == product.id }.aliases)
    assertTrue(resolved.vertices.single { it.id == product.id }.isProductModule)
    assertFalse(resolved.vertices.single { it.id == platform.id }.isProductModule)
  }

  private fun assertModularGraph(fixture: ModularDependenciesFixture, repeatMainDependencies: Boolean) {
    val graph = fixture.graph
    val root = DependencyNode.PluginDependency(fixture.plugin)
    val core = DependencyNode.ModuleDependency(fixture.plugin, "owner.core")
    val extra = DependencyNode.ModuleDependency(fixture.plugin, "owner.extra")
    val platform = DependencyNode.PluginDependency(fixture.platform)
    val bundled = DependencyNode.PluginDependency(fixture.bundled)
    val transitive = DependencyNode.PluginDependency(fixture.transitive)
    val expectedEdges = linkedSetOf(
      DependencyEdge(root, core, PluginDependencyImpl("owner.core", false, true)),
      DependencyEdge(root, extra, PluginDependencyImpl("owner.extra", false, true)),
      DependencyEdge(core, platform, PluginDependencyImpl("com.intellij.modules.platform", false, true)),
      DependencyEdge(extra, core, PluginDependencyImpl("owner.core", false, true)),
      DependencyEdge(extra, bundled, PluginDependencyImpl("bundled", false, false)),
      DependencyEdge(bundled, transitive, PluginDependencyImpl("transitive", false, false))
    )
    if (repeatMainDependencies) {
      expectedEdges += DependencyEdge(root, platform, PluginDependencyImpl("com.intellij.modules.platform", false, true))
      expectedEdges += DependencyEdge(root, bundled, PluginDependencyImpl("bundled", false, false))
    }

    assertEquals(setOf(
      Dependency.Module(fixture.platform, "com.intellij.modules.platform"),
      Dependency.Plugin(fixture.bundled),
      Dependency.Plugin(fixture.transitive, isTransitive = true)
    ), fixture.resolution.transitiveDependencies.toSet())
    assertTrue(fixture.resolution.missingDependencies.isEmpty())
    assertEquals(root, graph.verifiedPlugin)
    assertEquals(setOf(root, core, extra, platform, bundled, transitive), graph.vertices)
    assertEquals(expectedEdges, graph.edges)
    assertTrue(graph.missingDependencies.isEmpty())
    assertEquals(setOf(core, extra), graph.vertices.filterIsInstance<DependencyNode.ModuleDependency>().toSet())
    graph.vertices.filterIsInstance<DependencyNode.ModuleDependency>().forEach {
      assertSame(fixture.plugin, it.plugin)
      assertEquals("1.0", it.version)
    }
    assertEquals(setOf("com.intellij.modules.platform"), (graph.vertices.single { it == platform } as DependencyNode.PluginDependency).aliases)
    assertTrue(graph.edges.none { it.from == it.to })
    val cycles = mutableListOf<List<DependencyNode>>()
    graph.checkForCycle { cycles += it }
    assertTrue("Sibling module dependencies must not become a verified-plugin cycle: $cycles", cycles.isEmpty())

    val resolved = graph.toResolved()
    assertEquals(setOf(
      ResolvedDependencyNode("owner", "1.0"),
      ResolvedDependencyNode("owner.core", "1.0", moduleOwnerId = "owner"),
      ResolvedDependencyNode("owner.extra", "1.0", moduleOwnerId = "owner"),
      ResolvedDependencyNode("com.intellij", "261.1", setOf("com.intellij.modules.platform")),
      ResolvedDependencyNode("bundled", "2.0"),
      ResolvedDependencyNode("transitive", "3.0")
    ), resolved.vertices)
    assertEquals(graph.edges.map { edge ->
      Triple(edge.from.toString(), edge.to.toString(), ResolvedPluginDependency(edge.dependency.id, edge.dependency.isOptional, edge.dependency.isModule))
    }.toSet(), resolved.edges.map { Triple(it.from.toString(), it.to.toString(), it.dependency) }.toSet())
    assertEquals(graph.edges.size, resolved.edges.size)
  }
}

internal data class ModularDependenciesFixture(
  val plugin: MockIdePlugin,
  val platform: MockIdePlugin,
  val bundled: MockIdePlugin,
  val transitive: MockIdePlugin,
  val resolution: DependencyTreeResolution
) {
  val graph = DependenciesGraphProvider().getDependenciesGraph(resolution)
}

internal fun modularDependenciesFixture(repeatMainDependencies: Boolean): ModularDependenciesFixture {
  val platform = MockIdePlugin(
    pluginId = "com.intellij",
    pluginVersion = "261.1",
    pluginAliases = setOf("com.intellij.modules.platform")
  )
  val transitive = MockIdePlugin(pluginId = "transitive", pluginVersion = "3.0")
  val bundled = MockIdePlugin(
    pluginId = "bundled",
    pluginVersion = "2.0",
    dependsList = listOf(DependsPluginDependency("transitive", false))
  )
  val coreDependencies = listOf(PluginV1Dependency.Mandatory("com.intellij.modules.platform"))
  val extraDependencies = listOf(ModuleV2Dependency("owner.core"), PluginV1Dependency.Mandatory("bundled"))
  val core = contentModuleDescriptor("owner.core", coreDependencies, if (repeatMainDependencies) emptyList() else coreDependencies)
  val extra = contentModuleDescriptor("owner.extra", extraDependencies, if (repeatMainDependencies) extraDependencies.take(1) else extraDependencies)
  val plugin = MockIdePlugin(
    pluginId = "owner",
    pluginVersion = "1.0",
    dependsList = if (repeatMainDependencies) listOf(
      DependsPluginDependency("com.intellij.modules.platform", false),
      DependsPluginDependency("bundled", false)
    ) else emptyList(),
    contentModules = listOf(core.moduleDefinition, extra.moduleDefinition),
    modulesDescriptors = listOf(core, extra)
  )
  val ide = MockIde(IdeVersion.createIdeVersion("IU-261.1"), bundledPlugins = listOf(platform, bundled, transitive))
  val resolution = DependencyTree(ide, HAS_COM_INTELLIJ_MODULE_PREFIX).getDependencyTreeResolution(
    plugin, DefaultDependencyContributor(includeContentModuleDependencies = true)
  )
  return ModularDependenciesFixture(plugin, platform, bundled, transitive, resolution)
}

internal fun legacyDependenciesGraph(): DependenciesGraph {
  val platform = MockIdePlugin(
    pluginId = "com.intellij",
    pluginVersion = "261.1",
    pluginAliases = setOf("com.intellij.modules.platform"),
    contentModules = listOf(Module.FileBasedModule("com.intellij.modules.platform", null, "jetbrains", ModuleLoadingRule.REQUIRED, "platform.xml"))
  )
  val product = IdeModule("com.intellij.modules.product", "261.1", hasPackagePrefix = false)
  val plugin = MockIdePlugin(
    pluginId = "legacy",
    pluginVersion = "1.0",
    dependsList = listOf(
      DependsPluginDependency("com.intellij.modules.platform", false),
      DependsPluginDependency("com.intellij.modules.product", false)
    )
  )
  val ide = MockIde(IdeVersion.createIdeVersion("IU-261.1"), bundledPlugins = listOf(platform, product))
  val resolution = DependencyTree(ide, HAS_COM_INTELLIJ_MODULE_PREFIX).getDependencyTreeResolution(plugin)
  return DependenciesGraphProvider().getDependenciesGraph(resolution)
}

private fun contentModuleDescriptor(
  name: String,
  declaredDependencies: List<PluginDependency>,
  resolvedDependencies: List<PluginDependency>
): ModuleDescriptor = ModuleDescriptor.of(
  module = MockIdePlugin(pluginId = name, pluginVersion = "module descriptor version"),
  moduleDefinition = Module.FileBasedModule(name, null, "jetbrains", ModuleLoadingRule.REQUIRED, "$name.xml"),
  resolvedDependencies = resolvedDependencies,
  declaredDependencies = declaredDependencies
)
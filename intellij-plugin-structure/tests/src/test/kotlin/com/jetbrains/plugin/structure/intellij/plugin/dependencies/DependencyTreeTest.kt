package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.IdPrefixIdeModulePredicate.Companion.HAS_COM_INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy.LegacyPluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class DependencyTreeTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  private lateinit var tenIjDependencies: List<MockIdePlugin>

  private lateinit var ijPlugin: MockIdePlugin

  private lateinit var dozenOfPlugins: List<MockIdePlugin>

  private lateinit var pluginAlpha: MockIdePlugin

  private lateinit var ide: MockIde

  private lateinit var pluginNotInIde: MockIdePlugin

  private lateinit var somePlugin: MockIdePlugin

  @Before
  fun setUp() {
    ideRoot = temporaryFolder.newFolder("idea").toPath()

    tenIjDependencies = (1..10).map {
      MockIdePlugin(pluginId = "ij-dependency-$it")
    }

    ijPlugin = MockIdePlugin("ij", dependencies = tenIjDependencies.map { dependOn(it.pluginId!!) })

    dozenOfPlugins = (1..12).map {
      MockIdePlugin(pluginId = "plugin$it", dependencies = listOf(dependOn("ij")))
    }

    pluginAlpha = MockIdePlugin(pluginId = "alpha", dependencies =
      dozenOfPlugins.map { dependOn(it.pluginId!!) }
    )

    ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, listOf(pluginAlpha, ijPlugin) + dozenOfPlugins + tenIjDependencies)

    pluginNotInIde = MockIdePlugin(pluginId = "notInIde", dependencies = listOf(dependOn("pluginAlpha")))

    somePlugin = MockIdePlugin(pluginId = "com.example.A", dependencies = listOf(dependOn("alpha"), dependOn(pluginNotInIde.pluginId!!)))
  }

  @Test
  fun `dependency tree is correct`() {
    val dependencyTree = DependencyTree(ide)
    val expectedDependencies = setOf(
      Dependency.Plugin(pluginAlpha)) +
      // pluginNotInIde is not in the IDE, has been excluded
      dozenOfPlugins.map { Dependency.Plugin(it, isTransitive = true) } +
      Dependency.Plugin(ijPlugin, isTransitive = true) +
      tenIjDependencies.map { Dependency.Plugin(it, isTransitive = true) }

    val actualDependencies = dependencyTree.getTransitiveDependencies(somePlugin)
    assertEquals(expectedDependencies, actualDependencies)
  }

  @Test
  fun `missing dependencies are collected`() {
    val dependencyTree = DependencyTree(ide)
    val missingDependencies = MissingDependencyCollector()
    dependencyTree.getTransitiveDependencies(somePlugin, missingDependencies)

    val expectedPluginDependency = PluginV1Dependency.Mandatory(pluginNotInIde.pluginId!!)
    assertEquals(setOf(expectedPluginDependency), missingDependencies)
  }

  @Test
  fun `missing optional dependency`() {
    val optionalPlugin = MockIdePlugin(pluginId = "com.example.Optional")
    val somePlugin = MockIdePlugin(pluginId = "com.example.A", dependencies = listOf(optionallyDependOn(optionalPlugin)))
    // optionalPlugin is not in the IDE
    val bundledPlugins = emptyList<IdePlugin>()
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, bundledPlugins)

    val dependencyTree = DependencyTree(ide)
    val missingDependencies = MissingDependencyCollector()
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(somePlugin, missingDependencies)

    assertEquals(emptySet<Dependency>(), transitiveDependencies)

    val missingOptionalDependency = PluginDependencyImpl(optionalPlugin.id, true, false)
    assertEquals(setOf(missingOptionalDependency), missingDependencies)
  }

  @Test
  fun `missing transitive optional dependency`() {
    val optionalPlugin = MockIdePlugin(pluginId = "com.example.Optional")
    val alphaPlugin = MockIdePlugin(pluginId = "alpha", dependencies = listOf(optionallyDependOn(optionalPlugin)))
    val somePlugin = MockIdePlugin(pluginId = "com.example.A", dependencies = listOf(dependOn(alphaPlugin)))

    // optionalPlugin is not in the IDE
    val bundledPlugins = listOf(alphaPlugin)
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, bundledPlugins)

    val dependencyTree = DependencyTree(ide)
    val missingDependencies = MissingDependencyCollector()
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(somePlugin, missingDependencies)

    val expectedTransitiveDependencies = setOf(
      Dependency.Plugin(alphaPlugin, isTransitive = false),
      // optionalPlugin is not in the IDE
    )
    assertEquals(expectedTransitiveDependencies, transitiveDependencies)

    val missingOptionalDependency = PluginDependencyImpl(optionalPlugin.id, true, false)
    assertEquals(setOf(missingOptionalDependency), missingDependencies)
  }

  @Test
  fun `plugin has no dependencies`() {
    val noDependenciesPlugin = MockIdePlugin(pluginId = "com.example.NoDependencies")

    val dependencyTree = DependencyTree(ide)

    val transitiveDependencies = dependencyTree.getTransitiveDependencies(noDependenciesPlugin)
    assertEquals(emptySet<Dependency>(), transitiveDependencies)
  }

  @Test
  fun `plugin has no dependencies but dependency modifier for legacy plugins adds Java module`() {
    val javaPlugin = MockIdePlugin(
      pluginName = "Java",
      pluginId = "com.intellij.java",
      pluginAliases = setOf("com.intellij.modules.java")
    )
    val bundledPlugins = listOf(
      MockIdePlugin(pluginId = "com.intellij", pluginAliases = setOf("com.intellij.modules.all")),
      javaPlugin
    )
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, bundledPlugins)

    val legacyPlugin = MockIdePlugin(pluginId = "com.example.Legacy")
    val legacyPluginVerifier = LegacyIntelliJIdeaPluginVerifier()
    val legacyPluginDependencyContributor = LegacyPluginDependencyContributor(ide, legacyPluginVerifier)
    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)

    val transitiveDependencies =
      dependencyTree.getTransitiveDependencies(legacyPlugin, dependenciesModifier = legacyPluginDependencyContributor)

    val expectedJavaDependency = Dependency.Module(javaPlugin, isTransitive = false, id = "com.intellij.modules.java")
    with(transitiveDependencies) {
      assertEquals(1, size)
      assertEquals(expectedJavaDependency, transitiveDependencies.first())
    }
  }

  @Test
  fun `standard plugin has no Java plugin contributed from to legacy rule`() {
    val javaModuleName = "com.intellij.modules.java"
    val javaPlugin = MockIdePlugin(pluginId = "Java", pluginAliases = setOf(javaModuleName))
    val platformPlugin = MockIdePlugin(pluginId = "com.intellij", pluginAliases = setOf("com.intellij.modules.all", "com.intellij.modules.platform"))
    val bundledPlugins = listOf(platformPlugin, javaPlugin)
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, bundledPlugins)

    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)

    val somePlugin = MockIdePlugin(pluginId = "com.example.A", dependencies = listOf(dependOnModule(platformPlugin, via = "com.intellij.modules.platform")))

    val legacyPluginVerifier = LegacyIntelliJIdeaPluginVerifier()
    val transitiveDependencies =
      dependencyTree.getTransitiveDependencies(somePlugin, dependenciesModifier = LegacyPluginDependencyContributor(ide, legacyPluginVerifier))
    with(transitiveDependencies) {
      assertEquals(1, size)

      val expectedPlatformDependency =
        Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false)
      assertEquals(expectedPlatformDependency, transitiveDependencies.first())
    }
  }

  @Test
  fun `dependency tree resolution is correctly resolved`() {
    val dependencyTree = DependencyTree(ide)
    val dependencyTreeResolution = dependencyTree.getDependencyTreeResolution(somePlugin)

    with(dependencyTreeResolution) {
      val expectedDependencyIds = mutableSetOf<PluginId>().apply {
        this += tenIjDependencies.map { it.pluginId!! }
        this += ijPlugin.pluginId!!
        this += dozenOfPlugins.map { it.pluginId!! }
        this += pluginAlpha.pluginId!!
      }

      assertSetsEqual(expectedDependencyIds, transitiveDependencies.map { it.id }.toSet())

      val expectedMissingDependencies = mapOf(
        somePlugin to setOf(PluginV1Dependency.Mandatory (pluginNotInIde.pluginId!!)))
      assertEquals(expectedMissingDependencies, this.missingDependencies)
    }
  }

  @Test
  fun `transitive dependencies are resolved from dependency tree resolution`() {
    val dependencyTree = DependencyTree(ide)
    val dependencyTreeResolution = dependencyTree.getDependencyTreeResolution(somePlugin)

    val expectedDependencies = setOf(
      Dependency.Plugin(pluginAlpha)) +
      // pluginNotInIde is not in the IDE, has been excluded
      dozenOfPlugins.map { Dependency.Plugin(it, isTransitive = true) } +
      Dependency.Plugin(ijPlugin, isTransitive = true) +
      tenIjDependencies.map { Dependency.Plugin(it, isTransitive = true) }

    val expectedDependencyIdentifiers = mutableSetOf<PluginId>().apply {
      this += expectedDependencies.map { it.plugin.pluginId!! }
    }

    if (dependencyTreeResolution is DefaultDependencyTreeResolution) {
      val transitiveDepIds = dependencyTreeResolution.transitiveDependencies.map { it.id }
      val expectedTransitiveDeps = expectedDependencyIdentifiers - somePlugin.pluginId!!
      assertSetsEqual(expectedTransitiveDeps, transitiveDepIds.toSet())
    }

    assertEquals(expectedDependencyIdentifiers, dependencyTreeResolution.transitiveDependencies.map { it.id }.toSet())
  }

  fun <T> assertSetsEqual(expected: Set<T>, actual: Set<T>) {
    val missing = expected - actual
    val extra = actual - expected

    if (missing.isNotEmpty() || extra.isNotEmpty()) {
      val message = buildString {
        appendLine("Sets are not equal.")
        if (missing.isNotEmpty()) {
          appendLine("Missing elements: $missing")
        }
        if (extra.isNotEmpty()) {
          appendLine("Extra elements: $extra")
        }
      }
      fail(message)
    }
  }

  private val MockIdePlugin.id: String
    get() = pluginId ?: pluginName ?: "unknown"

  private fun optionallyDependOn(plugin: MockIdePlugin): PluginDependencyImpl {
    return optionallyDependOn(plugin.id)
  }

  private fun optionallyDependOn(id: String): PluginDependencyImpl {
    return PluginDependencyImpl(id, true, false)
  }

  private fun dependOn(plugin: MockIdePlugin): PluginDependency {
    return dependOn(plugin.id)
  }

  private fun dependOn(id: String): PluginDependency {
    return PluginV1Dependency.Mandatory(id)
  }

  private fun dependOnModule(@Suppress("unused") module: MockIdePlugin, via: String): PluginDependency {
    return PluginV1Dependency.Mandatory(via)
  }

  class MissingDependencyCollector(private val missingDependencies: MutableSet<PluginDependency> = mutableSetOf()) : MissingDependencyListener, Set<PluginDependency> {
    override fun invoke(plugin: IdePlugin, dependency: PluginDependency) {
      missingDependencies += dependency
    }

    override val size: Int
      get() = missingDependencies.size

    override fun contains(element: PluginDependency) = missingDependencies.contains(element)

    override fun containsAll(elements: Collection<PluginDependency>) = missingDependencies.containsAll(elements)

    override fun isEmpty() = missingDependencies.isEmpty()

    override fun iterator() = missingDependencies.iterator()

    override fun toString() = missingDependencies.toString()
  }

}
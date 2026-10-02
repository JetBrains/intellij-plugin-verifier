package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.IdPrefixIdeModulePredicate.Companion.HAS_COM_INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy.LegacyPluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MandatoryV1Dependency
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.idePlugin
import com.jetbrains.plugin.structure.mocks.validation.MockIdePluginValidator.Companion.assertValid
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

  private lateinit var platformPlugin: MockIdePlugin

  private lateinit var platformPluginWithContentModule: MockIdePlugin

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

    platformPlugin = MockIdePlugin("com.intellij", "IDEA CORE", pluginAliases = setOf("com.intellij.modules.platform"))

    tenIjDependencies = (1..10).map {
      idePlugin("ij-dependency-$it")
    }

    ijPlugin = idePlugin("ij") {
      tenIjDependencies.forEach { depends(it) }
    }

    dozenOfPlugins = (1..12).map {
      idePlugin("plugin$it") {
        depends("ij")
      }
    }

    pluginAlpha = idePlugin("alpha") {
      dozenOfPlugins.forEach { depends(it) }
    }

    platformPluginWithContentModule = run {
      val moduleDefinition = InlineModule(CORE_CONTENT_MODULE_IN_A_BUNDLED_PLUGIN, null, "jetbrains", ModuleLoadingRule.REQUIRED, "")
      val contentModule = MockIdePlugin(
        dependsList = listOf(MandatoryV1Dependency("com.intellij.modules.platform")),
      )
      MockIdePlugin(
        pluginId = "com.intellij.bundledModularPlugin",
        contentModules = listOf(moduleDefinition),
        modulesDescriptors = listOf(
          ModuleDescriptor.of(
            contentModule,
            moduleDefinition,
            resolvedDependencies = contentModule.reconstructDependencies(),
          ),
        ),
      ).assertValid()
    }

    ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, listOf(platformPlugin)
      + listOf(pluginAlpha, ijPlugin, platformPluginWithContentModule)
      + dozenOfPlugins + tenIjDependencies)

    pluginNotInIde = idePlugin("notInIde") {
      depends("pluginAlpha")
    }

    somePlugin = idePlugin("com.example.A") {
      depends("alpha")
      depends(pluginNotInIde)
    }
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
    val optionalPlugin = idePlugin("com.example.Optional")
    val somePlugin = idePlugin("com.example.A") {
      optionalDepends(optionalPlugin)
    }
    // optionalPlugin is not in the IDE
    val bundledPlugins = emptyList<IdePlugin>()
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, bundledPlugins)

    val dependencyTree = DependencyTree(ide)
    val missingDependencies = MissingDependencyCollector()
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(somePlugin, missingDependencies)

    assertEquals(emptySet<Dependency>(), transitiveDependencies)

    val missingOptionalDependency = PluginV1Dependency.Optional(optionalPlugin.pluginId!!)
    assertEquals(setOf(missingOptionalDependency), missingDependencies)
  }

  @Test
  fun `missing transitive optional dependency`() {
    val optionalPlugin = idePlugin("com.example.Optional")
    val alphaPlugin = idePlugin("alpha") {
      optionalDepends(optionalPlugin)
    }
    val somePlugin = idePlugin("com.example.A") {
      depends(alphaPlugin)
    }

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

    val missingOptionalDependency = PluginV1Dependency.Optional(optionalPlugin.pluginId!!)
    assertEquals(setOf(missingOptionalDependency), missingDependencies)
  }

  @Test
  fun `platform constraints remain part of dependency resolution by default`() {
    val platformConstraints = listOf(
      PluginV1Dependency.Mandatory("com.intellij.modules.os.mac"),
      PluginV1Dependency.Mandatory("com.intellij.modules.arch.arm64")
    )
    val osArchConstrainedPlugin = idePlugin("com.example.OsArch") {
      depends("com.intellij.modules.os.mac")
      depends("com.intellij.modules.arch.arm64")
    }

    val resolution = DependencyTree(ide).getDependencyTreeResolution(osArchConstrainedPlugin)
    assertEquals(mapOf(osArchConstrainedPlugin to platformConstraints.toSet()), resolution.missingDependencies)
  }

  @Test
  fun `plugin has no dependencies`() {
    val noDependenciesPlugin = idePlugin("com.example.NoDependencies").assertValid()

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
    ).assertValid()
    val bundledPlugins = listOf(
      MockIdePlugin(pluginId = "com.intellij", pluginAliases = setOf("com.intellij.modules.all")).assertValid(),
      javaPlugin.assertValid()
    )
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, bundledPlugins)

    val legacyPlugin = idePlugin("com.example.Legacy")
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
    val javaPlugin = MockIdePlugin(pluginId = "Java", pluginAliases = setOf(javaModuleName)).assertValid()
    val platformPlugin = MockIdePlugin(pluginId = "com.intellij", pluginAliases = setOf("com.intellij.modules.all", "com.intellij.modules.platform")).assertValid()
    val bundledPlugins = listOf(platformPlugin, javaPlugin)
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, bundledPlugins)

    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)

    val somePlugin = idePlugin("com.example.A") {
      depends("com.intellij.modules.platform")
    }

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
        somePlugin to setOf(PluginV1Dependency.Mandatory(pluginNotInIde.pluginId!!)))
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

  @Test
  fun `plugin with content modules reaches out to transitive dependencies`() {
    val someBundledIdePlugin = dozenOfPlugins.first()

    val coreModule = idePlugin("core") {
      depends("com.intellij.modules.platform")
    }
    val extrasModule = idePlugin("extras") {
      moduleDependency("core")
      depends(someBundledIdePlugin)
    }

    val pluginWithContentModules = MockIdePlugin(
      pluginId = "com.example.Modular",
      // FIXME add contentModules property
      modulesDescriptors = listOf(
        ModuleDescriptor.of(
          coreModule,
          InlineModule("core", null, "com.example", ModuleLoadingRule.REQUIRED, ""),
          resolvedDependencies = coreModule.reconstructDependencies(),
        ),
        ModuleDescriptor.of(
          extrasModule,
          InlineModule("extras", null, "com.example", ModuleLoadingRule.REQUIRED, ""),
          resolvedDependencies = extrasModule.reconstructDependencies(),
        ),
      ),
      dependsList = listOf(
        MandatoryV1Dependency("com.intellij.modules.platform"),
        MandatoryV1Dependency(someBundledIdePlugin.id)
      ),
      contentModuleDependencies = listOf(
        ContentModuleDependency("core", "com.example"),
      ),
    )

    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)

    val transitiveDependencies = dependencyTree.getTransitiveDependencies(pluginWithContentModules)

    val ijPluginDependencies = tenIjDependencies.map { Dependency.Plugin(it, isTransitive = true) }

    val expectedDependencies = setOf(
      Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false),
      Dependency.Plugin(someBundledIdePlugin, isTransitive = false),
      Dependency.Plugin(ijPlugin, isTransitive = true)
    ) + ijPluginDependencies

    assertSetsEqual(expectedDependencies, transitiveDependencies)

    val dependencyTreeString = dependencyTree.toString(pluginWithContentModules).toString()
    val expectedDependencyTreeString = """
      * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij'
      * Plugin dependency: 'plugin1'
        * Plugin dependency: 'ij'
          * Plugin dependency: 'ij-dependency-1'
          * Plugin dependency: 'ij-dependency-10'
          * Plugin dependency: 'ij-dependency-2'
          * Plugin dependency: 'ij-dependency-3'
          * Plugin dependency: 'ij-dependency-4'
          * Plugin dependency: 'ij-dependency-5'
          * Plugin dependency: 'ij-dependency-6'
          * Plugin dependency: 'ij-dependency-7'
          * Plugin dependency: 'ij-dependency-8'
          * Plugin dependency: 'ij-dependency-9'

    """.trimIndent()
    assertEquals(expectedDependencyTreeString, dependencyTreeString)
  }

  @Test
  fun `plugin with content modules depends on a content module in a bundled plugin`() {
    val coreContentModuleDefinition =
      InlineModule("com.example.thirdPartyModularPlugin.core", null, "com.example", ModuleLoadingRule.REQUIRED, "")

    val coreContentModule = idePlugin("com.example.thirdPartyModularPlugin.core") {
      depends("com.intellij.modules.platform")
      // depend on a content module in a bundled plugin
      moduleDependency(CORE_CONTENT_MODULE_IN_A_BUNDLED_PLUGIN, "jetbrains")
    }

    val pluginWithContentModules = MockIdePlugin(
      pluginId = "com.example.thirdPartyModularPlugin",
      contentModules = listOf(coreContentModuleDefinition),
      modulesDescriptors = listOf(
        ModuleDescriptor.of(
          coreContentModule,
          coreContentModuleDefinition,
          resolvedDependencies = coreContentModule.reconstructDependencies(),
        ),
      )
    ).assertValid()

    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)

    val dependencyContributor = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(pluginWithContentModules, dependenciesModifier = dependencyContributor)

    val expectedDependencies = setOf(
      Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false),
      Dependency.Module(platformPluginWithContentModule, CORE_CONTENT_MODULE_IN_A_BUNDLED_PLUGIN, isTransitive = false)
    )

    assertSetsEqual(expectedDependencies, transitiveDependencies)

    val dependencyTreeString = dependencyTree.toString(pluginWithContentModules).toString()
    val expectedDependencyTreeString = """
      * Module '$CORE_CONTENT_MODULE_IN_A_BUNDLED_PLUGIN' provided by plugin 'com.intellij.bundledModularPlugin'
        * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij'
      * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij' (already visited)

    """.trimIndent()
    assertEquals(expectedDependencyTreeString, dependencyTreeString)
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

private const val CORE_CONTENT_MODULE_IN_A_BUNDLED_PLUGIN = "com.intellij.bundledModularPlugin.core"

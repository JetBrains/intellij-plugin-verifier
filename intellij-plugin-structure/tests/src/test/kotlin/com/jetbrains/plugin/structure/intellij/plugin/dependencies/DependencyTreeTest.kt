package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.Module.InlineModule
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.IdPrefixIdeModulePredicate.Companion.HAS_COM_INTELLIJ_MODULE_PREFIX
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy.LegacyPluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.*
import com.jetbrains.plugin.structure.mocks.validation.MockIdePluginValidator.Companion.assertValid
import org.junit.Assert.*
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

    assertSetsEqual(expectedDependencies, dependencyTreeResolution.transitiveDependencies.toSet())

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
  fun `plugin with content modules includes transitive dependencies of direct plugin dependencies`() {
    val someBundledIdePlugin = dozenOfPlugins.first()

    val coreContentModule = contentModule("com.example.Modular.core") {
      depends("com.intellij.modules.platform")
    }
    val coreContentModuleDefinition = InlineModule("com.example.Modular.core", null, "com.example", ModuleLoadingRule.REQUIRED, "")
    val coreContentModuleDescriptor = ModuleDescriptor.of(
      coreContentModule,
      coreContentModuleDefinition,
      resolvedDependencies = coreContentModule.reconstructDependencies(),
    )

    val extrasContentModule = contentModule("com.example.Modular.extras") {
      moduleDependency("com.example.Modular.core")
      depends(someBundledIdePlugin)
    }
    val extrasContentModuleDefinition = InlineModule("com.example.Modular.extras", null, "com.example", ModuleLoadingRule.REQUIRED, "")
    val extrasContentModuleDescriptor = ModuleDescriptor.of(
      extrasContentModule,
      extrasContentModuleDefinition,
      resolvedDependencies = extrasContentModule.reconstructDependencies(),
    )

    val pluginWithContentModules = MockIdePlugin(
      pluginId = "com.example.Modular",
      contentModules = listOf(coreContentModuleDefinition, extrasContentModuleDefinition),
      modulesDescriptors = listOf(
        coreContentModuleDescriptor,
        extrasContentModuleDescriptor,
      ),
      dependsList = listOf(
        MandatoryV1Dependency("com.intellij.modules.platform"),
        MandatoryV1Dependency(someBundledIdePlugin.id)
      ),
      contentModuleDependencies = listOf(
        ContentModuleDependency("core", "com.example"),
      ),
    ).assertValid()

    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)

    val dependencyContributor = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(pluginWithContentModules)
    val resolution = dependencyTree.getDependencyTreeResolution(pluginWithContentModules, dependencyContributor)

    val ijPluginDependencies = tenIjDependencies.map { Dependency.Plugin(it, isTransitive = true) }

    val expectedDependencies = setOf(
      Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false),
      Dependency.Plugin(someBundledIdePlugin, isTransitive = false),
      Dependency.Plugin(ijPlugin, isTransitive = true)
    ) + ijPluginDependencies

    assertEquals(13, transitiveDependencies.size)
    assertSetsEqual(expectedDependencies, transitiveDependencies)
    assertSetsEqual(expectedDependencies, dependencyTree.getTransitiveDependencies(pluginWithContentModules, dependenciesModifier = dependencyContributor))
    assertEquals(13, resolution.transitiveDependencies.size)
    assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())

    val rootNode = NodeId.ofPlugin(pluginWithContentModules)
    val coreNode = NodeId(rootNode.pluginId, coreContentModuleDefinition.name)
    val extrasNode = NodeId(rootNode.pluginId, extrasContentModuleDefinition.name)
    val platformNode = Dependency.Module(platformPlugin, "com.intellij.modules.platform").nodeId
    val bundledNode = NodeId.ofPlugin(someBundledIdePlugin)
    val ijNode = NodeId.ofPlugin(ijPlugin)
    val expectedEdges = mapOf(
      rootNode to setOf(coreNode, extrasNode, platformNode, bundledNode),
      coreNode to setOf(platformNode),
      extrasNode to setOf(coreNode, bundledNode),
      bundledNode to setOf(ijNode),
      ijNode to tenIjDependencies.map { NodeId.ofPlugin(it) }.toSet()
    )
    assertEquals(expectedEdges, resolution.graphEdges())

    val dependencyTreeString = dependencyTree.toString(pluginWithContentModules).toString()
    val expectedDependencyTreeString = """
      * Content module 'com.example.Modular.core' declared by plugin 'com.example.Modular'
        * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij'
      * Content module 'com.example.Modular.extras' declared by plugin 'com.example.Modular'
        * Module 'com.example.Modular.core' provided by plugin 'com.example.Modular' (already visited)
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
      * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij' (already visited)
      * Plugin dependency: 'plugin1' (already visited)

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
    val resolution = dependencyTree.getDependencyTreeResolution(pluginWithContentModules, dependencyContributor)
    assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())

    val dependencyTreeString = dependencyTree.toString(pluginWithContentModules).toString()
    val expectedDependencyTreeString = """
      * Content module 'com.example.thirdPartyModularPlugin.core' declared by plugin 'com.example.thirdPartyModularPlugin'
        * Module '$CORE_CONTENT_MODULE_IN_A_BUNDLED_PLUGIN' provided by plugin 'com.intellij.bundledModularPlugin'
          * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij'
        * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij' (already visited)

    """.trimIndent()
    assertEquals(expectedDependencyTreeString, dependencyTreeString)
  }

  @Test
  fun `single core content module resolves bundled core and transitive-only target`() {
    val target = idePlugin("com.example.TransitiveOnlyTarget")
    val bundledCore = contentModule("com.intellij.bundledPlugin.core") {
      depends(target)
    }
    val bundledPlugin = modularPlugin("com.intellij.bundledPlugin", bundledCore)
    val consumerCore = contentModule("com.example.Consumer.core") {
      moduleDependency(bundledCore.pluginId!!, "com.example")
    }
    val consumer = modularPlugin("com.example.Consumer", consumerCore)
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, listOf(bundledPlugin, target))
    val contributor = DefaultDependencyContributor(includeContentModuleDependencies = true)

    for (owner in listOf(consumer, bundledPlugin)) {
      assertEquals(1, owner.contentModules.size)
      assertEquals(1, owner.modulesDescriptors.size)
      assertTrue(owner.dependsList.isEmpty())
      assertTrue(owner.pluginMainModuleDependencies.isEmpty())
      assertTrue(owner.contentModuleDependencies.isEmpty())
      assertTrue(owner.pluginAliases.isEmpty())
    }
    val provision = ide.query(PluginQuery.Builder.of(bundledCore.pluginId!!).inContentModuleId().build())
    assertTrue(provision is PluginProvision.Found)
    provision as PluginProvision.Found
    assertEquals(bundledPlugin, provision.plugin)
    assertEquals(PluginProvision.Source.CONTENT_MODULE_ID, provision.source)

    val consumerNode = NodeId.ofPlugin(consumer)
    val consumerCoreNode = NodeId(consumerNode.pluginId, consumerCore.pluginId!!)
    val bundledCoreNode = NodeId(bundledPlugin.pluginId!!, bundledCore.pluginId)
    val expectedEdges = mapOf(
      consumerNode to setOf(consumerCoreNode),
      consumerCoreNode to setOf(bundledCoreNode),
      bundledCoreNode to setOf(NodeId.ofPlugin(target))
    )
    val expectedDependencies = setOf(
      Dependency.Module(bundledPlugin, bundledCore.pluginId, isTransitive = false),
      Dependency.Plugin(target, isTransitive = true)
    )

    for (dependencyTree in listOf(DependencyTree(ide), DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX))) {
      val missingDependencies = MissingDependencyCollector()
      val dependencies = dependencyTree.getTransitiveDependencies(consumer, missingDependencies, contributor)
      assertSetsEqual(expectedDependencies, dependencies)
      assertTrue(missingDependencies.isEmpty())

      val resolution = dependencyTree.getDependencyTreeResolution(consumer, contributor)
      assertEquals(expectedDependencies.size, resolution.transitiveDependencies.size)
      assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())
      assertTrue(resolution.missingDependencies.isEmpty())
      assertEquals(expectedEdges, resolution.graphEdges())
    }
  }

  @Test
  fun `module-only dependencies retain graph sources and flattened directness`() {
    val someBundledIdePlugin = dozenOfPlugins.first()
    val coreModule = contentModule("com.example.ModuleOnly.core") {
      depends("com.intellij.modules.platform")
    }
    val extrasModule = contentModule("com.example.ModuleOnly.extras") {
      moduleDependency(coreModule.pluginId!!, "com.example")
      depends(someBundledIdePlugin)
    }
    val plugin = modularPlugin("com.example.ModuleOnly", coreModule, extrasModule)
    assertTrue(plugin.dependsList.isEmpty())
    assertTrue(plugin.pluginMainModuleDependencies.isEmpty())
    assertTrue(plugin.contentModuleDependencies.isEmpty())

    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)
    val contributor = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val resolution = dependencyTree.getDependencyTreeResolution(plugin, contributor)
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(plugin, dependenciesModifier = contributor)
    val expectedDependencies = setOf(
      Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false),
      Dependency.Plugin(someBundledIdePlugin, isTransitive = false),
      Dependency.Plugin(ijPlugin, isTransitive = true)
    ) + tenIjDependencies.map { Dependency.Plugin(it, isTransitive = true) }

    assertEquals(13, transitiveDependencies.size)
    assertSetsEqual(expectedDependencies, transitiveDependencies)
    assertEquals(13, resolution.transitiveDependencies.size)
    assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())
    assertTrue(resolution.missingDependencies.isEmpty())

    val rootNode = NodeId.ofPlugin(plugin)
    val coreNode = NodeId(rootNode.pluginId, coreModule.pluginId!!)
    val extrasNode = NodeId(rootNode.pluginId, extrasModule.pluginId!!)
    val platformNode = Dependency.Module(platformPlugin, "com.intellij.modules.platform").nodeId
    val bundledNode = NodeId.ofPlugin(someBundledIdePlugin)
    val ijNode = NodeId.ofPlugin(ijPlugin)
    val edges = mutableListOf<Pair<Dependency, Dependency>>()
    resolution.forEach { from, to -> edges += from to to }
    assertEquals(setOf(
      Dependency.ContentModuleDeclaration(plugin, coreModule.pluginId),
      Dependency.ContentModuleDeclaration(plugin, extrasModule.pluginId)
    ), edges.filter { it.first.nodeId == rootNode }.map { it.second }.toSet())
    assertEquals(Dependency.Module(plugin, coreModule.pluginId),
                 edges.single { it.first.nodeId == extrasNode && it.second.nodeId == coreNode }.second)
    val expectedEdges = mapOf(
      rootNode to setOf(coreNode, extrasNode),
      coreNode to setOf(platformNode),
      extrasNode to setOf(coreNode, bundledNode),
      bundledNode to setOf(ijNode),
      ijNode to tenIjDependencies.map { NodeId.ofPlugin(it) }.toSet()
    )
    assertEquals(expectedEdges, resolution.graphEdges())
  }

  @Test
  fun `sibling cycles resolve against owner descriptors before provider modules`() {
    val coreModuleId = "com.example.Cyclic.core"
    val extrasModuleId = "com.example.Cyclic.extras"
    val coreModule = contentModule(coreModuleId) {
      moduleDependency(extrasModuleId, "com.example")
      depends("com.intellij.modules.platform")
    }
    val extrasModule = contentModule(extrasModuleId) {
      moduleDependency(coreModuleId, "com.example")
    }
    val plugin = modularPlugin("com.example.Cyclic", coreModule, extrasModule)
    val otherProvider = MockIdePlugin(
      pluginId = "com.example.OtherProvider",
      pluginAliases = setOf(coreModuleId, extrasModuleId)
    ).assertValid()
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, listOf(platformPlugin, otherProvider))
    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)
    val contributor = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val resolution = dependencyTree.getDependencyTreeResolution(plugin, contributor)
    val expectedDependencies = setOf(Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false))

    assertSetsEqual(expectedDependencies, dependencyTree.getTransitiveDependencies(plugin, dependenciesModifier = contributor))
    assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())
    assertTrue(resolution.missingDependencies.isEmpty())

    val rootNode = NodeId.ofPlugin(plugin)
    val coreNode = NodeId(rootNode.pluginId, coreModuleId)
    val extrasNode = NodeId(rootNode.pluginId, extrasModuleId)
    val platformNode = Dependency.Module(platformPlugin, "com.intellij.modules.platform").nodeId
    assertEquals(mapOf(
      rootNode to setOf(coreNode, extrasNode),
      coreNode to setOf(extrasNode, platformNode),
      extrasNode to setOf(coreNode)
    ), resolution.graphEdges())

    val expectedDependencyTreeString = """
      * Content module '$coreModuleId' declared by plugin 'com.example.Cyclic'
        * Module '$extrasModuleId' provided by plugin 'com.example.Cyclic'
          * Module '$coreModuleId' provided by plugin 'com.example.Cyclic' (already visited)
        * Module 'com.intellij.modules.platform' provided by plugin 'com.intellij'
      * Content module '$extrasModuleId' declared by plugin 'com.example.Cyclic' (already visited)

    """.trimIndent()
    assertEquals(expectedDependencyTreeString, dependencyTree.toString(plugin).toString())
  }

  @Test
  fun `dependency filter removes module declarations but preserves ownership edges`() {
    val bundledPlugin = dozenOfPlugins.first()
    val coreModule = contentModule("com.example.Filtered.core") {
      depends("com.intellij.modules.platform")
    }
    val missingPluginId = "com.example.Filtered.missing"
    val extrasModule = contentModule("com.example.Filtered.extras") {
      moduleDependency(coreModule.pluginId!!, "com.example")
      depends(bundledPlugin)
      optionalDepends(missingPluginId)
    }
    val plugin = modularPlugin("com.example.Filtered", coreModule, extrasModule)
    val filteredIds = setOf(coreModule.pluginId!!, bundledPlugin.pluginId!!, missingPluginId)
    val dependencyTree = DependencyTree(ide, HAS_COM_INTELLIJ_MODULE_PREFIX) { it.id !in filteredIds }
    val contributor = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val resolution = dependencyTree.getDependencyTreeResolution(plugin, contributor)
    val missingDependencies = MissingDependencyCollector()
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(plugin, missingDependencies, contributor)
    val expectedDependencies = setOf(Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false))

    assertSetsEqual(expectedDependencies, transitiveDependencies)
    assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())
    assertTrue(missingDependencies.isEmpty())
    assertTrue(resolution.missingDependencies.isEmpty())

    val rootNode = NodeId.ofPlugin(plugin)
    val coreNode = NodeId(rootNode.pluginId, coreModule.pluginId)
    val extrasNode = NodeId(rootNode.pluginId, extrasModule.pluginId!!)
    val platformNode = Dependency.Module(platformPlugin, "com.intellij.modules.platform").nodeId
    assertEquals(mapOf(
      rootNode to setOf(coreNode, extrasNode),
      coreNode to setOf(platformNode)
    ), resolution.graphEdges())
  }

  @Test
  fun `missing module dependencies are reported for the owner without losing sibling edges`() {
    val coreModule = contentModule("com.example.Missing.core") {
      depends("com.intellij.modules.platform")
    }
    val missingPluginId = "com.example.Missing.required"
    val missingOptionalPluginId = "com.example.Missing.optional"
    val extrasModule = contentModule("com.example.Missing.extras") {
      moduleDependency(coreModule.pluginId!!, "com.example")
      depends(missingPluginId)
      optionalDepends(missingOptionalPluginId)
    }
    val plugin = modularPlugin("com.example.Missing", coreModule, extrasModule)
    val dependencyTree = DependencyTree(ide, ideModulePredicate = HAS_COM_INTELLIJ_MODULE_PREFIX)
    val contributor = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val resolution = dependencyTree.getDependencyTreeResolution(plugin, contributor)
    val missingDependencies = MissingDependencyCollector()
    val transitiveDependencies = dependencyTree.getTransitiveDependencies(plugin, missingDependencies, contributor)
    val expectedDependencies = setOf(Dependency.Module(platformPlugin, "com.intellij.modules.platform", isTransitive = false))
    val expectedMissingDependencies = setOf(
      PluginV1Dependency.Mandatory(missingPluginId),
      PluginV1Dependency.Optional(missingOptionalPluginId)
    )

    assertSetsEqual(expectedDependencies, transitiveDependencies)
    assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())
    assertEquals(expectedMissingDependencies, missingDependencies)
    assertEquals(mapOf(plugin to expectedMissingDependencies), resolution.missingDependencies)

    val rootNode = NodeId.ofPlugin(plugin)
    val coreNode = NodeId(rootNode.pluginId, coreModule.pluginId!!)
    val extrasNode = NodeId(rootNode.pluginId, extrasModule.pluginId!!)
    val platformNode = Dependency.Module(platformPlugin, "com.intellij.modules.platform").nodeId
    assertEquals(mapOf(
      rootNode to setOf(coreNode, extrasNode),
      coreNode to setOf(platformNode),
      extrasNode to setOf(coreNode)
    ), resolution.graphEdges())
  }

  @Test
  fun `shared provider plugin and module nodes are all expanded regardless of entry order`() {
    val sharedDependency = idePlugin("com.example.SharedDependency")
    val coreDependency = idePlugin("com.example.CoreDependency")
    val extrasDependency = idePlugin("com.example.ExtrasDependency")
    val coreModule = contentModule("com.example.Provider.core") {
      depends(sharedDependency)
      depends(coreDependency)
    }
    val extrasModule = contentModule("com.example.Provider.extras") {
      depends(sharedDependency)
      depends(extrasDependency)
    }
    val provider = modularPlugin("com.example.Provider", coreModule, extrasModule).copy(
      dependsList = listOf(MandatoryV1Dependency(sharedDependency.pluginId!!))
    ).assertValid()
    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot,
      listOf(provider, sharedDependency, coreDependency, extrasDependency))
    val contributor = DefaultDependencyContributor(includeContentModuleDependencies = true)

    for (pluginFirst in listOf(true, false)) {
      val plugin = idePlugin("com.example.Consumer.$pluginFirst") {
        if (pluginFirst) depends(provider) else pluginDependency(provider.pluginId!!)
        moduleDependency(coreModule.pluginId!!, "com.example")
        moduleDependency(extrasModule.pluginId!!, "com.example")
      }
      val dependencyTree = DependencyTree(ide)
      val resolution = dependencyTree.getDependencyTreeResolution(plugin, contributor)
      val transitiveDependencies = dependencyTree.getTransitiveDependencies(plugin, dependenciesModifier = contributor)
      val expectedDependencies = setOf(
        Dependency.Plugin(provider, isTransitive = false),
        Dependency.Module(provider, coreModule.pluginId!!, isTransitive = false),
        Dependency.Module(provider, extrasModule.pluginId!!, isTransitive = false),
        Dependency.Plugin(sharedDependency, isTransitive = true),
        Dependency.Plugin(coreDependency, isTransitive = true),
        Dependency.Plugin(extrasDependency, isTransitive = true)
      )
      assertSetsEqual(expectedDependencies, transitiveDependencies)
      assertEquals(expectedDependencies.size, resolution.transitiveDependencies.size)
      assertSetsEqual(expectedDependencies, resolution.transitiveDependencies.toSet())
      assertTrue(resolution.missingDependencies.isEmpty())

      val rootNode = NodeId.ofPlugin(plugin)
      val providerNode = NodeId.ofPlugin(provider)
      val coreNode = NodeId(providerNode.pluginId, coreModule.pluginId)
      val extrasNode = NodeId(providerNode.pluginId, extrasModule.pluginId)
      val sharedNode = NodeId.ofPlugin(sharedDependency)
      val edges = resolution.graphEdges()
      assertEquals(setOf(providerNode, coreNode, extrasNode), edges[rootNode])
      assertTrue(edges[providerNode]!!.containsAll(setOf(coreNode, extrasNode, sharedNode)))
      assertTrue(edges[coreNode]!!.containsAll(setOf(sharedNode, NodeId.ofPlugin(coreDependency))))
      assertTrue(edges[extrasNode]!!.containsAll(setOf(sharedNode, NodeId.ofPlugin(extrasDependency))))
    }
  }

  private fun modularPlugin(pluginId: String, vararg modules: MockIdePlugin): MockIdePlugin {
    val descriptors = modules.map { module ->
      val definition = InlineModule(module.pluginId!!, null, "com.example", ModuleLoadingRule.REQUIRED, "")
      ModuleDescriptor.of(module, definition, resolvedDependencies = module.reconstructDependencies())
    }
    return MockIdePlugin(
      pluginId = pluginId,
      contentModules = descriptors.map { it.moduleDefinition },
      modulesDescriptors = descriptors
    ).assertValid()
  }

  private fun DependencyTreeResolution.graphEdges(): Map<NodeId, Set<NodeId>> {
    val edges = linkedMapOf<NodeId, MutableSet<NodeId>>()
    forEach { from, to ->
      edges.getOrPut(requireNotNull(from.nodeId)) { linkedSetOf() } += requireNotNull(to.nodeId)
    }
    return edges
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

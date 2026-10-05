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

  @Test
  fun `mandatory dependencies win over optional duplicates independently of reasons`() {
    val plugin = MockIdePlugin(pluginId = "com.example.plugin")
    val mandatoryDependency = PluginV1Dependency.Mandatory("com.example.shared")
    val optionalDependency = mandatoryDependency.asOptional()
    for (mandatoryReason in DependencyModificationReason.values()) {
      for (optionalReason in DependencyModificationReason.values()) {
        val mandatory = DependencyModification(mandatoryDependency, mandatoryReason)
        val optional = DependencyModification(optionalDependency, optionalReason)
        for (output in listOf(listOf(optional, mandatory), listOf(mandatory, optional))) {
          val modifiedDependencies = CompositeDependenciesModifier(
            { _, _ -> output }
          ).apply(plugin, ide)

          assertEquals(
            "$output",
            DependencyModification(
              mandatoryDependency,
              maxOf(mandatoryReason, optionalReason),
              output.flatMap { it.contributions }
            ),
            modifiedDependencies.single()
          )
        }
      }
    }
  }

  @Test
  fun `mandatory dependencies win across stages independently of reasons`() {
    val plugin = MockIdePlugin(pluginId = "com.example.plugin")
    val mandatoryDependency = PluginV1Dependency.Mandatory("com.example.shared")
    val optionalDependency = mandatoryDependency.asOptional()
    for (mandatoryReason in DependencyModificationReason.values()) {
      for (optionalReason in DependencyModificationReason.values()) {
        val mandatory = DependencyModification(mandatoryDependency, mandatoryReason)
        val optional = DependencyModification(optionalDependency, optionalReason)
        for (stages in listOf(listOf(optional, mandatory), listOf(mandatory, optional))) {
          val modifiedDependencies = CompositeDependenciesModifier(
            { _, _ -> listOf(stages.first()) },
            { _, _ -> listOf(stages.last()) }
          ).apply(plugin, ide)

          assertEquals("$stages", mandatoryDependency, modifiedDependencies.single().dependency)
          assertEquals(maxOf(mandatoryReason, optionalReason), modifiedDependencies.single().reason)
          assertEquals(stages.last().contributions, modifiedDependencies.single().contributions)
        }
      }
    }
  }

  @Test
  fun `duplicate ids retain first mandatory dependency and first occurrence order`() {
    val plugin = MockIdePlugin("com.example.plugin")
    for (isOptional in listOf(false, true)) {
      val first = PluginV2Dependency("com.example.shared", isOptional)
      val second = ModuleV2Dependency("com.example.shared", isOptional)
      val unrelated = DependencyModification(PluginV2Dependency("com.example.unrelated"), DependencyModificationReason.IDE)
      val output = listOf(
        DependencyModification(first, DependencyModificationReason.OTHER),
        unrelated,
        DependencyModification(second, DependencyModificationReason.CONTENT_MODULE)
      )

      val modifiedDependencies = CompositeDependenciesModifier(
        { _, _ -> output }
      ).apply(plugin, ide)

      assertEquals(listOf(first, unrelated.dependency), modifiedDependencies.map { it.dependency })
      assertEquals(DependencyModificationReason.CONTENT_MODULE, modifiedDependencies.first().reason)
    }
  }

  @Test
  fun `default contributor preserves main and module sources for shared dependencies`() {
    val plugin = pluginWithSharedDependencies()

    val modifiedDependencies = PassThruDependenciesModifier.apply(plugin, ide)

    assertSharedContributions(modifiedDependencies)
    assertEquals(plugin.dependencies, modifiedDependencies.map { it.dependency })
  }

  @Test
  fun `required module declaration does not strengthen optional main dependency`() {
    val plugin = pluginWithOptionalSharedV1Dependency()

    val modifiedDependencies = CompositeDependenciesModifier(
      PassThruDependenciesModifier
    ).apply(plugin, ide)

    assertEquals(plugin.dependencies, modifiedDependencies.map { it.dependency })
    val sharedDependency = modifiedDependencies.single { it.dependency.id == "shared.plugin" }
    assertEquals(PluginV1Dependency.Optional("shared.plugin"), sharedDependency.dependency)
    assertEquals(DependencyModificationReason.PLUGIN, sharedDependency.reason)
    assertEquals(
      listOf(
        PluginMainModuleDependencyContribution(PluginV1Dependency.Optional("shared.plugin")),
        ContentModuleDependencyContribution("example.required", PluginV2Dependency("shared.plugin")),
        ContentModuleDependencyContribution("example.optional", inlinePluginDependency("shared.plugin"))
      ),
      sharedDependency.contributions
    )
  }

  @Test
  fun `default contributor without content modules retains only main sources`() {
    val plugin = pluginWithSharedDependencies()
    val mainOnly = DependenciesModifier { pluginView, _ ->
      pluginView.reconstructDependencies().map {
        val reason = if (it is ModuleV2Dependency) {
          DependencyModificationReason.CONTENT_MODULE
        } else {
          DependencyModificationReason.PLUGIN
        }
        DependencyModification(it, reason)
      }
    }

    val modifiedDependencies = mainOnly.apply(plugin, ide)

    assertEquals(
      listOf(
        DependencyModification(ModuleV2Dependency("shared.module"), DependencyModificationReason.CONTENT_MODULE),
        DependencyModification(PluginV2Dependency("shared.plugin"), DependencyModificationReason.PLUGIN)
      ),
      modifiedDependencies
    )
    assertEquals(plugin.reconstructDependencies(), modifiedDependencies.map { it.dependency })
    modifiedDependencies.forEach { modification ->
      assertEquals(listOf(PluginMainModuleDependencyContribution(modification.dependency)), modification.contributions)
    }
  }

  @Test
  fun `empty composite preserves initial main and module contributions`() {
    val plugin = pluginWithSharedDependencies()

    val modifiedDependencies = CompositeDependenciesModifier(emptyList()).apply(plugin, ide)

    assertSharedContributions(modifiedDependencies)
    assertEquals(plugin.dependencies, modifiedDependencies.map { it.dependency })
  }

  @Test
  fun `pass-through and chained modifiers preserve sources optionality and reasons`() {
    val plugin = pluginWithSharedDependencies()
    val passThroughDependencies = PassThruDependenciesModifier.apply(plugin, ide)
    val compositeModifier = CompositeDependenciesModifier(
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier),
      { pluginView, pluginProvider ->
        PassThruDependenciesModifier.apply(pluginView, pluginProvider).map {
          it.copy(reason = DependencyModificationReason.OTHER)
        }
      },
      PassThruDependenciesModifier
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertSharedContributions(passThroughDependencies)
    assertEquals(passThroughDependencies, modifiedDependencies)
  }

  @Test
  fun `composite merges a new module source without changing legacy dependency or reason`() {
    val plugin = pluginWithSharedDependencies()
    val addedDependency = PluginV2Dependency("shared.plugin", isOptional = true)
    val addedContribution = ContentModuleDependencyContribution("example.additional", addedDependency)
    val compositeModifier = CompositeDependenciesModifier(
      { pluginView, pluginProvider ->
        PassThruDependenciesModifier.apply(pluginView, pluginProvider) + DependencyModification(
          addedDependency, DependencyModificationReason.OTHER, listOf(addedContribution)
        )
      },
      PassThruDependenciesModifier
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(plugin.dependencies, modifiedDependencies.map { it.dependency })
    val sharedDependency = modifiedDependencies.single { it.dependency.id == "shared.plugin" }
    assertEquals(DependencyModificationReason.PLUGIN, sharedDependency.reason)
    assertEquals(
      listOf(
        PluginMainModuleDependencyContribution(PluginV2Dependency("shared.plugin")),
        ContentModuleDependencyContribution("example.required", PluginV2Dependency("shared.plugin")),
        ContentModuleDependencyContribution("example.optional", inlinePluginDependency("shared.plugin")),
        addedContribution
      ),
      sharedDependency.contributions
    )
  }

  @Test
  fun `equal optionality replacements are independent of historical reason priority`() {
    val plugin = MockIdePlugin(pluginId = "com.example.plugin")
    for (isOptional in listOf(false, true)) {
      val previousDependency = PluginV2Dependency("com.example.shared", isOptional)
      val incomingDependency = ModuleV2Dependency("com.example.shared", isOptional)
      for (incomingReason in DependencyModificationReason.values()) {
        val incoming = DependencyModification(incomingDependency, incomingReason)
        val modifiedDependencies = CompositeDependenciesModifier(
          { _, _ -> listOf(DependencyModification(previousDependency, DependencyModificationReason.CONTENT_MODULE)) },
          { _, _ -> listOf(incoming) },
          PassThruDependenciesModifier
        ).apply(plugin, ide)

        assertEquals(
          listOf(incoming.copy(reason = DependencyModificationReason.CONTENT_MODULE)),
          modifiedDependencies
        )
      }
    }
  }

  @Test
  fun `removing one module source survives pass-through and nested composites`() {
    val plugin = pluginWithSharedDependencies()
    val initial = PassThruDependenciesModifier.apply(plugin, ide)
    val removeOptionalModule = DependenciesModifier { pluginView, pluginProvider ->
      PassThruDependenciesModifier.apply(pluginView, pluginProvider).map { modification ->
        modification.copy(contributions = modification.contributions.filterNot {
          it is ContentModuleDependencyContribution && it.contributingContentModule == "example.optional"
        })
      }
    }
    val compositeModifier = CompositeDependenciesModifier(
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(removeOptionalModule, PassThruDependenciesModifier),
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier)
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(
      initial.map { modification -> modification.copy(contributions = modification.contributions.filterNot {
        it is ContentModuleDependencyContribution && it.contributingContentModule == "example.optional"
      }) },
      modifiedDependencies
    )
  }

  @Test
  fun `reconstructing modifications replaces module sources with default main sources`() {
    val plugin = pluginWithSharedDependencies()
    val initial = PassThruDependenciesModifier.apply(plugin, ide)
    val mainOnly = DependenciesModifier { pluginView, _ ->
      pluginView.dependencies.map { DependencyModification(it, DependencyModificationReason.OTHER) }
    }
    val compositeModifier = CompositeDependenciesModifier(
      mainOnly,
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier)
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(
      initial.map { it.copy(contributions = listOf(PluginMainModuleDependencyContribution(it.dependency))) },
      modifiedDependencies
    )
  }

  @Test
  fun `explicitly empty sources survive pass-through and nested composites`() {
    val plugin = pluginWithSharedDependencies()
    val initial = PassThruDependenciesModifier.apply(plugin, ide)
    val noSources = DependenciesModifier { pluginView, pluginProvider ->
      PassThruDependenciesModifier.apply(pluginView, pluginProvider).map { it.copy(contributions = emptyList()) }
    }
    val compositeModifier = CompositeDependenciesModifier(
      noSources,
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier)
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(initial.map { it.copy(contributions = emptyList()) }, modifiedDependencies)
  }

  @Test
  fun `duplicate ids aggregate only returned sources including explicit main sources`() {
    val plugin = pluginWithSharedDependencies()
    val initial = PassThruDependenciesModifier.apply(plugin, ide).single { it.dependency.id == "module.only.plugin" }
    val optionalContribution = initial.contributions.last()
    val mainContribution = PluginMainModuleDependencyContribution(initial.dependency)
    val output = listOf(
      DependencyModification(optionalContribution.dependency, DependencyModificationReason.OTHER, listOf(optionalContribution)),
      DependencyModification(initial.dependency, DependencyModificationReason.IDE, listOf(mainContribution, optionalContribution))
    )
    val compositeModifier = CompositeDependenciesModifier(
      { _, _ -> output },
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier)
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(
      listOf(initial.copy(contributions = listOf(optionalContribution, mainContribution))),
      modifiedDependencies
    )
  }

  @Test
  fun `composite filtering removes all sources of an id without resurrecting dependencies`() {
    val plugin = pluginWithSharedDependencies()
    val compositeModifier = CompositeDependenciesModifier(
      { pluginView, pluginProvider ->
        PassThruDependenciesModifier.apply(pluginView, pluginProvider).filterNot { it.dependency.id == "shared.plugin" }
      },
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier)
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(
      PassThruDependenciesModifier.apply(plugin, ide).filterNot { it.dependency.id == "shared.plugin" },
      modifiedDependencies
    )
    assertTrue(modifiedDependencies.none { modification ->
      modification.contributions.any { it.dependency.id == "shared.plugin" }
    })
  }

  /**
   * Creates a plugin whose main descriptor, required file-based module, and optional inline module declare
   * `shared.plugin` and `shared.module`. Both content modules also declare `module.only.plugin`.
   * The main descriptor declares `shared.plugin` as a mandatory V2 plugin dependency.
   * Only the module-only dependency is resolved in the content modules, preserving shared declarations
   * for tests of dependency contributions without changing the merged dependencies.
   */
  private fun pluginWithSharedDependencies(): IdePlugin {
    val requiredModule = Module.FileBasedModule(
      "example.required", null, "jetbrains", ModuleLoadingRule.REQUIRED, "example.required.xml"
    )
    val optionalModule = Module.InlineModule(
      "example.optional", null, "jetbrains", ModuleLoadingRule.OPTIONAL, "<idea-plugin/>"
    )
    return MockIdePlugin(
      pluginId = "com.example.plugin",
      pluginMainModuleDependencies = listOf(PluginMainModuleDependency("shared.plugin")),
      contentModuleDependencies = listOf(ContentModuleDependency("shared.module", "jetbrains")),
      contentModules = listOf(requiredModule, optionalModule),
      modulesDescriptors = listOf(
        ModuleDescriptor.of(
          idePlugin("example.required") {
            pluginDependency("shared.plugin")
            moduleDependency("shared.module")
            pluginDependency("module.only.plugin")
          },
          requiredModule,
          resolvedDependencies = listOf(PluginV2Dependency("module.only.plugin")),
          declaredDependencies = listOf(
            PluginV2Dependency("shared.plugin"),
            ModuleV2Dependency("shared.module"),
            PluginV2Dependency("module.only.plugin")
          )
        ),
        ModuleDescriptor.of(
          idePlugin("example.optional") {
            pluginDependency("shared.plugin")
            moduleDependency("shared.module")
            pluginDependency("module.only.plugin")
          },
          optionalModule,
          resolvedDependencies = listOf(inlinePluginDependency("module.only.plugin")),
          declaredDependencies = listOf(
            inlinePluginDependency("shared.plugin"),
            InlineDeclaredModuleV2Dependency.Module("shared.module", true, "com.example.plugin", "example.optional"),
            inlinePluginDependency("module.only.plugin")
          )
        )
      )
    )
  }

  /**
   * Creates a plugin whose main descriptor declares `shared.plugin` as an optional V1 dependency,
   * without a V2 plugin dependency in the main descriptor. The required file-based module and optional
   * inline module also declare `shared.plugin`, while all three descriptors declare `shared.module`.
   * Both content modules declare `module.only.plugin`, which is their only resolved dependency,
   * so the required module's shared declaration does not strengthen the optional main dependency.
   */
  private fun pluginWithOptionalSharedV1Dependency(): IdePlugin {
    val requiredModule = Module.FileBasedModule(
      "example.required", null, "jetbrains", ModuleLoadingRule.REQUIRED, "example.required.xml"
    )
    val optionalModule = Module.InlineModule(
      "example.optional", null, "jetbrains", ModuleLoadingRule.OPTIONAL, "<idea-plugin/>"
    )
    return MockIdePlugin(
      pluginId = "com.example.plugin",
      dependsList = listOf(DependsPluginDependency("shared.plugin", isOptional = true)),
      contentModuleDependencies = listOf(ContentModuleDependency("shared.module", "jetbrains")),
      contentModules = listOf(requiredModule, optionalModule),
      modulesDescriptors = listOf(
        ModuleDescriptor.of(
          idePlugin("example.required") {
            pluginDependency("shared.plugin")
            moduleDependency("shared.module")
            pluginDependency("module.only.plugin")
          },
          requiredModule,
          resolvedDependencies = listOf(PluginV2Dependency("module.only.plugin")),
          declaredDependencies = listOf(
            PluginV2Dependency("shared.plugin"),
            ModuleV2Dependency("shared.module"),
            PluginV2Dependency("module.only.plugin")
          )
        ),
        ModuleDescriptor.of(
          idePlugin("example.optional") {
            pluginDependency("shared.plugin")
            moduleDependency("shared.module")
            pluginDependency("module.only.plugin")
          },
          optionalModule,
          resolvedDependencies = listOf(inlinePluginDependency("module.only.plugin")),
          declaredDependencies = listOf(
            inlinePluginDependency("shared.plugin"),
            InlineDeclaredModuleV2Dependency.Module("shared.module", true, "com.example.plugin", "example.optional"),
            inlinePluginDependency("module.only.plugin")
          )
        )
      )
    )
  }

  private fun assertSharedContributions(modifiedDependencies: List<DependencyModification>) {
    assertEquals(3, modifiedDependencies.size)
    assertEquals(
      listOf(
        DependencyModification(
          ModuleV2Dependency("shared.module"),
          DependencyModificationReason.CONTENT_MODULE,
          listOf(
            PluginMainModuleDependencyContribution(ModuleV2Dependency("shared.module")),
            ContentModuleDependencyContribution("example.required", ModuleV2Dependency("shared.module")),
            ContentModuleDependencyContribution(
              "example.optional",
              InlineDeclaredModuleV2Dependency.Module("shared.module", true, "com.example.plugin", "example.optional")
            )
          )
        ),
        DependencyModification(
          PluginV2Dependency("shared.plugin"),
          DependencyModificationReason.PLUGIN,
          listOf(
            PluginMainModuleDependencyContribution(PluginV2Dependency("shared.plugin")),
            ContentModuleDependencyContribution("example.required", PluginV2Dependency("shared.plugin")),
            ContentModuleDependencyContribution("example.optional", inlinePluginDependency("shared.plugin"))
          )
        ),
        DependencyModification(
          PluginV2Dependency("module.only.plugin"),
          DependencyModificationReason.PLUGIN,
          listOf(
            ContentModuleDependencyContribution("example.required", PluginV2Dependency("module.only.plugin")),
            ContentModuleDependencyContribution("example.optional", inlinePluginDependency("module.only.plugin"))
          )
        )
      ),
      modifiedDependencies
    )
  }

  private fun inlinePluginDependency(id: String) =
    InlineDeclaredModuleV2Dependency.Plugin(id, true, "com.example.plugin", "example.optional")

  private fun List<DependencyModification>.reasonOf(id: String) = first { it.dependency.id == id }.reason
}
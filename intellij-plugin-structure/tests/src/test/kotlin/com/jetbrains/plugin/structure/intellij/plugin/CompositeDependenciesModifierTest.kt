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
  fun `last stage dependency and reason win for the same dependency id`() {
    val sharedDependencyId = "com.example.shared"
    val plugin = MockIdePlugin(pluginId = "com.example.plugin")
    val lastStage = DependencyModification(
      PluginV1Dependency.Mandatory(sharedDependencyId), DependencyModificationReason.OTHER
    )
    val compositeModifier = CompositeDependenciesModifier(
      FixedDependenciesModifier(
        DependencyModification(PluginV1Dependency.Mandatory(sharedDependencyId), DependencyModificationReason.IDE)
      ),
      FixedDependenciesModifier(
        DependencyModification(ModuleV2Dependency(sharedDependencyId), DependencyModificationReason.CONTENT_MODULE)
      ),
      FixedDependenciesModifier(
        DependencyModification(PluginV1Dependency.Mandatory(sharedDependencyId), DependencyModificationReason.PLUGIN)
      ),
      FixedDependenciesModifier(lastStage)
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(listOf(lastStage), modifiedDependencies)
  }

  @Test
  fun `last stage decides optionality in both directions`() {
    val plugin = MockIdePlugin(pluginId = "com.example.plugin")
    val mandatoryDependency = PluginV1Dependency.Mandatory("com.example.shared")
    val optionalDependency = mandatoryDependency.asOptional()
    for (mandatoryReason in DependencyModificationReason.entries) {
      for (optionalReason in DependencyModificationReason.entries) {
        val mandatory = DependencyModification(mandatoryDependency, mandatoryReason)
        val optional = DependencyModification(optionalDependency, optionalReason)
        // optional -> mandatory and mandatory -> optional
        for (stages in listOf(listOf(optional, mandatory), listOf(mandatory, optional))) {
          val modifiedDependencies = CompositeDependenciesModifier(
            FixedDependenciesModifier(stages.first()),
            FixedDependenciesModifier(stages.last()),
            PassThruDependenciesModifier
          ).apply(plugin, ide)

          assertEquals("$stages", listOf(stages.last()), modifiedDependencies)
        }
      }
    }
  }

  @Test
  fun `a later stage sees the previous stage result and can make a mandatory dependency optional`() {
    val plugin = idePlugin("com.example.plugin") {
      depends("com.example.shared")
    }
    val makeOptional = DependenciesModifier { pluginView, pluginProvider ->
      assertEquals(listOf(PluginV1Dependency.Mandatory("com.example.shared")), pluginView.dependencies)
      PassThruDependenciesModifier.apply(pluginView, pluginProvider).map {
        it.copy(dependency = it.dependency.asOptional(), reason = DependencyModificationReason.OTHER)
      }
    }

    val modifiedDependencies = CompositeDependenciesModifier(
      PassThruDependenciesModifier,
      makeOptional,
      PassThruDependenciesModifier
    ).apply(plugin, ide)

    val sharedDependency = modifiedDependencies.single()
    assertEquals(PluginV1Dependency.Optional("com.example.shared"), sharedDependency.dependency)
    assertTrue(sharedDependency.dependency.isOptional)
    assertEquals(DependencyModificationReason.OTHER, sharedDependency.reason)
  }

  @Test
  fun `duplicate ids in a stage are deduplicated and the last entry wins`() {
    val plugin = MockIdePlugin("com.example.plugin")
    for (isOptional in listOf(false, true)) {
      val first = PluginV2Dependency("com.example.shared", isOptional)
      val second = ModuleV2Dependency("com.example.shared", !isOptional)
      val unrelated = DependencyModification(PluginV2Dependency("com.example.unrelated"), DependencyModificationReason.IDE)
      val last = DependencyModification(
        second,
        DependencyModificationReason.OTHER,
        listOf(ContentModuleDependencyContribution("example.module", second))
      )
      val output = listOf(
        DependencyModification(first, DependencyModificationReason.CONTENT_MODULE),
        unrelated,
        last
      )

      val modifiedDependencies = CompositeDependenciesModifier(
        FixedDependenciesModifier(output),
        PassThruDependenciesModifier,
        CompositeDependenciesModifier(PassThruDependenciesModifier)
      ).apply(plugin, ide)

      assertEquals(listOf(unrelated, last), modifiedDependencies)
    }
  }

  @Test
  fun `a later stage sees deduplicated modifications of the previous stage`() {
    val plugin = MockIdePlugin("com.example.plugin")
    val mandatory = DependencyModification(PluginV1Dependency.Mandatory("com.example.shared"), DependencyModificationReason.PLUGIN)
    val optional = DependencyModification(PluginV1Dependency.Optional("com.example.shared"), DependencyModificationReason.OTHER)
    val observingStage = DependenciesModifier { pluginView, pluginProvider ->
      assertEquals(listOf(optional.dependency), pluginView.dependencies)
      PassThruDependenciesModifier.apply(pluginView, pluginProvider)
    }

    val modifiedDependencies = CompositeDependenciesModifier(
      FixedDependenciesModifier(mandatory, optional),
      observingStage
    ).apply(plugin, ide)

    assertEquals(listOf(optional), modifiedDependencies)
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
  fun `pass-through and chained modifiers preserve sources and optionality and carry the last reason`() {
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
    assertEquals(
      passThroughDependencies.map { it.copy(reason = DependencyModificationReason.OTHER) },
      modifiedDependencies
    )
  }

  @Test
  fun `adding a module source through copy keeps the existing dependency and reason`() {
    val plugin = pluginWithSharedDependencies()
    val addedDependency = PluginV2Dependency("shared.plugin", isOptional = true)
    val addedContribution = ContentModuleDependencyContribution("example.additional", addedDependency)
    val compositeModifier = CompositeDependenciesModifier(
      { pluginView, pluginProvider ->
        PassThruDependenciesModifier.apply(pluginView, pluginProvider).map {
          if (it.dependency.id == "shared.plugin") it.copy(contributions = it.contributions + addedContribution) else it
        }
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
  fun `equal optionality replacements keep the incoming reason`() {
    val plugin = MockIdePlugin(pluginId = "com.example.plugin")
    for (isOptional in listOf(false, true)) {
      val previousDependency = PluginV2Dependency("com.example.shared", isOptional)
      val incomingDependency = ModuleV2Dependency("com.example.shared", isOptional)
      for (incomingReason in DependencyModificationReason.entries) {
        val incoming = DependencyModification(incomingDependency, incomingReason)
        val modifiedDependencies = CompositeDependenciesModifier(
          FixedDependenciesModifier(DependencyModification(previousDependency, DependencyModificationReason.CONTENT_MODULE)),
          FixedDependenciesModifier(incoming),
          PassThruDependenciesModifier
        ).apply(plugin, ide)

        assertEquals(listOf(incoming), modifiedDependencies)
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
      initial.map {
        it.copy(
          reason = DependencyModificationReason.OTHER,
          contributions = listOf(PluginMainModuleDependencyContribution(it.dependency))
        )
      },
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

  /**
   * A test stage that ignores the plugin view and always returns the given [modifications].
   */
  private class FixedDependenciesModifier(private val modifications: List<DependencyModification>) : DependenciesModifier {
    constructor(vararg modifications: DependencyModification) : this(modifications.toList())

    override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider) = modifications
  }
}
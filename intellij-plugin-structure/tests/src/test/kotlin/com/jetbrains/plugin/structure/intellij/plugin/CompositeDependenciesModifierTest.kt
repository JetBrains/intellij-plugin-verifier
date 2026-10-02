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
  fun `default contributor preserves main and module sources for shared dependencies`() {
    val plugin = pluginWithSharedDependencies()

    val modifiedDependencies = DefaultDependencyContributor(true).apply(plugin, ide)

    assertSharedContributions(modifiedDependencies)
    assertEquals(plugin.dependencies, modifiedDependencies.map { it.dependency })
  }

  @Test
  fun `required module declaration does not strengthen optional main dependency`() {
    val plugin = pluginWithSharedDependencies(optionalMain = true)

    val modifiedDependencies = CompositeDependenciesModifier(
      DefaultDependencyContributor(true), PassThruDependenciesModifier
    ).apply(plugin, ide)

    assertEquals(plugin.dependencies, modifiedDependencies.map { it.dependency })
    val sharedDependency = modifiedDependencies.single { it.dependency.id == "shared.plugin" }
    assertEquals(PluginV1Dependency.Optional("shared.plugin"), sharedDependency.dependency)
    assertEquals(DependencyModificationReason.PLUGIN, sharedDependency.reason)
    assertEquals(
      listOf(
        DependencyContribution(null, PluginV1Dependency.Optional("shared.plugin")),
        DependencyContribution("example.required", PluginV2Dependency("shared.plugin")),
        DependencyContribution("example.optional", inlinePluginDependency("shared.plugin"))
      ),
      sharedDependency.contributions
    )
  }

  @Test
  fun `default contributor without content modules retains only main sources`() {
    val plugin = pluginWithSharedDependencies()

    val modifiedDependencies = DefaultDependencyContributor(false).apply(plugin, ide)

    assertEquals(plugin.reconstructDependencies(), modifiedDependencies.map { it.dependency })
    modifiedDependencies.forEach { modification ->
      assertEquals(listOf(DependencyContribution(null, modification.dependency)), modification.contributions)
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
      DefaultDependencyContributor(true),
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier),
      { pluginView, _ ->
        pluginView.dependencies.map { DependencyModification(it, DependencyModificationReason.OTHER) }
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
    val addedContribution = DependencyContribution("example.additional", addedDependency)
    val compositeModifier = CompositeDependenciesModifier(
      DefaultDependencyContributor(true),
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
        DependencyContribution(null, PluginV2Dependency("shared.plugin")),
        DependencyContribution("example.required", PluginV2Dependency("shared.plugin")),
        DependencyContribution("example.optional", inlinePluginDependency("shared.plugin")),
        addedContribution
      ),
      sharedDependency.contributions
    )
  }

  @Test
  fun `composite filtering removes all sources of an id without resurrecting dependencies`() {
    val plugin = pluginWithSharedDependencies()
    val compositeModifier = CompositeDependenciesModifier(
      DefaultDependencyContributor(true),
      { pluginView, pluginProvider ->
        PassThruDependenciesModifier.apply(pluginView, pluginProvider).filterNot { it.dependency.id == "shared.plugin" }
      },
      PassThruDependenciesModifier,
      CompositeDependenciesModifier(PassThruDependenciesModifier)
    )

    val modifiedDependencies = compositeModifier.apply(plugin, ide)

    assertEquals(
      DefaultDependencyContributor(true).apply(plugin, ide).filterNot { it.dependency.id == "shared.plugin" },
      modifiedDependencies
    )
    assertTrue(modifiedDependencies.none { modification ->
      modification.contributions.any { it.dependency.id == "shared.plugin" }
    })
  }

  private fun pluginWithSharedDependencies(optionalMain: Boolean = false): IdePlugin = IdePluginImpl().apply {
    pluginId = "com.example.plugin"
    if (optionalMain) {
      addDepends(DependsPluginDependency("shared.plugin", isOptional = true))
    } else {
      addPluginMainModuleDependency(PluginMainModuleDependency("shared.plugin"))
    }
    addContentModuleDependency(ContentModuleDependency("shared.module", "jetbrains"))
    val requiredModule = Module.FileBasedModule(
      "example.required", null, "jetbrains", ModuleLoadingRule.REQUIRED, "example.required.xml"
    )
    val optionalModule = Module.InlineModule(
      "example.optional", null, "jetbrains", ModuleLoadingRule.OPTIONAL, "<idea-plugin/>"
    )
    contentModules += listOf(requiredModule, optionalModule)
    modulesDescriptors += ModuleDescriptor.of(
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
    )
    modulesDescriptors += ModuleDescriptor.of(
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
  }

  private fun assertSharedContributions(modifiedDependencies: List<DependencyModification>) {
    assertEquals(3, modifiedDependencies.size)
    assertEquals(
      listOf(
        DependencyModification(
          ModuleV2Dependency("shared.module"),
          DependencyModificationReason.CONTENT_MODULE,
          listOf(
            DependencyContribution(null, ModuleV2Dependency("shared.module")),
            DependencyContribution("example.required", ModuleV2Dependency("shared.module")),
            DependencyContribution(
              "example.optional",
              InlineDeclaredModuleV2Dependency.Module("shared.module", true, "com.example.plugin", "example.optional")
            )
          )
        ),
        DependencyModification(
          PluginV2Dependency("shared.plugin"),
          DependencyModificationReason.PLUGIN,
          listOf(
            DependencyContribution(null, PluginV2Dependency("shared.plugin")),
            DependencyContribution("example.required", PluginV2Dependency("shared.plugin")),
            DependencyContribution("example.optional", inlinePluginDependency("shared.plugin"))
          )
        ),
        DependencyModification(
          PluginV2Dependency("module.only.plugin"),
          DependencyModificationReason.PLUGIN,
          listOf(
            DependencyContribution("example.required", PluginV2Dependency("module.only.plugin")),
            DependencyContribution("example.optional", inlinePluginDependency("module.only.plugin"))
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
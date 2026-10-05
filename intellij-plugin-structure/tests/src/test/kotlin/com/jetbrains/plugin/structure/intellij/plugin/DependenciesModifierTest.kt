package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.DependencyModificationReason.*
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.CorePluginDependencyContributor
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.idePlugin
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Executable examples: a modifier returns the complete dependency list, a modification explains why
 * a dependency is present, and contributions record the declarations behind that dependency.
 * A null contribution source denotes the main plugin rather than a named content module.
 */
class DependenciesModifierTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ide: MockIde

  @Before
  fun setUp() {
    ide = MockIde(
      IdeVersion.createIdeVersion("IU-261.1000"),
      temporaryFolder.newFolder("idea").toPath(),
      bundledPlugins = listOf(idePlugin("com.intellij"))
    )
  }

  @Test
  fun `a modification defaults to one main-plugin contribution but can name module sources`() {
    val dependency = PluginV2Dependency("com.example.language")

    val mainDeclaration = DependencyModification(dependency, PLUGIN)
    val moduleDeclaration = DependencyModification(
      dependency,
      PLUGIN,
      contributions = listOf(DependencyContribution("example.editor", dependency))
    )

    assertEquals(dependency, mainDeclaration.dependency)
    assertEquals(PLUGIN, mainDeclaration.reason)
    assertEquals(listOf(DependencyContribution(null, dependency)), mainDeclaration.contributions)
    assertEquals(dependency, moduleDeclaration.dependency)
    assertEquals(PLUGIN, moduleDeclaration.reason)
    assertEquals(listOf(DependencyContribution("example.editor", dependency)), moduleDeclaration.contributions)
  }

  @Test
  fun `pass-through keeps a single declared dependency`() {
    val plugin = idePlugin("com.example.editor") {
      depends("com.example.platform")
    }

    val modifications = PassThruDependenciesModifier.apply(plugin, ide)

    assertEquals(
      listOf(DependencyModification(PluginV1Dependency.Mandatory("com.example.platform"), PLUGIN)),
      modifications
    )
  }

  @Test
  fun `core contributor preserves declared dependencies and adds an implicit IDE dependency`() {
    val plugin = idePlugin("com.example.editor") {
      depends("com.example.platform")
    }
    val declaredDependency = PluginV1Dependency.Mandatory("com.example.platform")
    val coreDependency = PluginV1Dependency.Mandatory("com.intellij")

    val modifications = CorePluginDependencyContributor(ide).apply(plugin, ide)

    assertEquals(
      listOf(
        DependencyModification(declaredDependency, PLUGIN),
        DependencyModification(
          coreDependency,
          IDE,
          contributions = listOf(DependencyContribution(null, coreDependency))
        )
      ),
      modifications
    )
    assertEquals(listOf(declaredDependency), plugin.dependencies)
  }

  @Test
  fun `pass-through wraps V1 and V2 declarations with reasons and main-plugin sources`() {
    val plugin = idePlugin("com.example.editor") {
      depends("com.example.platform")
      optionalDepends("com.example.git")
      pluginDependency("com.example.language")
      moduleDependency("example.language.api")
    }
    val expected = listOf(
      DependencyModification(PluginV1Dependency.Mandatory("com.example.platform"), PLUGIN),
      DependencyModification(PluginV1Dependency.Optional("com.example.git"), PLUGIN),
      DependencyModification(ModuleV2Dependency("example.language.api"), CONTENT_MODULE),
      DependencyModification(PluginV2Dependency("com.example.language"), PLUGIN)
    )

    assertEquals(expected, PassThruDependenciesModifier.apply(plugin, ide))
    assertEquals(expected, DefaultDependencyContributor(includeContentModuleDependencies = false).apply(plugin, ide))
    assertEquals(expected, CompositeDependenciesModifier(emptyList()).apply(plugin, ide))
  }

  @Test
  fun `including content modules adds dependencies and retains required and optional declaration sources`() {
    val requiredModule = Module.FileBasedModule(
      "example.required", null, "jetbrains", ModuleLoadingRule.REQUIRED, "example.required.xml"
    )
    val optionalModule = Module.InlineModule(
      "example.optional", null, "jetbrains", ModuleLoadingRule.OPTIONAL, "<idea-plugin/>"
    )
    val requiredDeclaration = PluginV2Dependency("com.example.language")
    val optionalDeclaration = InlineDeclaredModuleV2Dependency.Plugin(
      "com.example.language", true, "com.example.editor", "example.optional"
    )
    val moduleOnlyDependency = InlineDeclaredModuleV2Dependency.Module(
      "example.git.api", true, "com.example.editor", "example.optional"
    )
    val plugin = idePlugin("com.example.editor") {
      optionalDepends("com.example.language")
    }.copy(
      contentModules = listOf(requiredModule, optionalModule),
      modulesDescriptors = listOf(
        ModuleDescriptor.of(
          idePlugin("example.required") { pluginDependency("com.example.language") },
          requiredModule,
          // Shared declarations have already been filtered out of the resolved module dependencies.
          resolvedDependencies = emptyList(),
          declaredDependencies = listOf(requiredDeclaration)
        ),
        ModuleDescriptor.of(
          idePlugin("example.optional") {
            pluginDependency("com.example.language")
            moduleDependency("example.git.api")
          },
          optionalModule,
          resolvedDependencies = listOf(moduleOnlyDependency),
          declaredDependencies = listOf(optionalDeclaration, moduleOnlyDependency)
        )
      )
    )
    val mainDependency = PluginV1Dependency.Optional("com.example.language")

    assertEquals(
      listOf(DependencyModification(mainDependency, PLUGIN)),
      DefaultDependencyContributor(includeContentModuleDependencies = false).apply(plugin, ide)
    )
    assertEquals(
      listOf(
        DependencyModification(
          mainDependency,
          PLUGIN,
          contributions = listOf(
            DependencyContribution(null, mainDependency),
            DependencyContribution("example.required", requiredDeclaration),
            DependencyContribution("example.optional", optionalDeclaration)
          )
        ),
        DependencyModification(
          moduleOnlyDependency,
          CONTENT_MODULE,
          contributions = listOf(DependencyContribution("example.optional", moduleOnlyDependency))
        )
      ),
      DefaultDependencyContributor(includeContentModuleDependencies = true).apply(plugin, ide)
    )
  }

  @Test
  fun `a custom additive modifier keeps existing modifications and appends an IDE dependency`() {
    val plugin = idePlugin("com.example.editor") { pluginDependency("com.example.language") }
    val addPlatform = DependenciesModifier { pluginView, pluginProvider ->
      PassThruDependenciesModifier.apply(pluginView, pluginProvider) +
        DependencyModification(PluginV1Dependency.Mandatory("com.intellij"), IDE)
    }

    assertEquals(
      listOf(
        DependencyModification(PluginV2Dependency("com.example.language"), PLUGIN),
        DependencyModification(PluginV1Dependency.Mandatory("com.intellij"), IDE)
      ),
      addPlatform.apply(plugin, ide)
    )
  }

  @Test
  @Suppress("DEPRECATION")
  fun `a composite chains declaration collection core contribution filtering and pass-through`() {
    val plugin = idePlugin("com.example.editor") {
      pluginDependency("com.example.language")
      optionalDepends("com.example.git")
    }
    val originalDependencies = plugin.dependencies
    val removeGit = DependenciesModifier { pluginView, pluginProvider ->
      val modifications = PassThruDependenciesModifier.apply(pluginView, pluginProvider)
      // This modifier sees the core dependency added by the previous modifier, with its IDE reason.
      assertEquals(
        DependencyModification(PluginV1Dependency.Mandatory("com.intellij"), IDE),
        modifications.single { it.dependency.id == "com.intellij" }
      )
      modifications.filterNot { it.dependency.id == "com.example.git" }
    }
    val modifier = CompositeDependenciesModifier(
      DefaultDependencyContributor(includeContentModuleDependencies = true),
      CorePluginDependencyContributor(ide),
      removeGit,
      PassThruDependenciesModifier
    )

    assertEquals(
      listOf(
        DependencyModification(PluginV2Dependency("com.example.language"), PLUGIN),
        DependencyModification(PluginV1Dependency.Mandatory("com.intellij"), IDE)
      ),
      modifier.apply(plugin, ide)
    )
    assertEquals(originalDependencies, plugin.dependencies)
  }

  @Test
  fun `returning an empty list removes all dependencies rather than making no changes`() {
    val plugin = idePlugin("com.example.editor") {
      pluginDependency("com.example.language")
    }
    val removeAll = DependenciesModifier { _, _ -> emptyList() }
    val modifier = CompositeDependenciesModifier(removeAll, PassThruDependenciesModifier)

    assertEquals(emptyList<DependencyModification>(), modifier.apply(plugin, ide))
  }

  @Test
  fun `duplicate ids keep the highest-priority reason and merge distinct contribution sources`() {
    val plugin = idePlugin("com.example.editor")
    val dependency = ModuleV2Dependency("example.platform.api")
    val optionalDependency = dependency.asOptional()
    val ideContribution = DependencyContribution(null, dependency)
    val requiredContribution = DependencyContribution("example.required", dependency)
    val optionalContribution = DependencyContribution("example.optional", optionalDependency)
    val modifier = CompositeDependenciesModifier(
      { _, _ -> listOf(DependencyModification(dependency, IDE, listOf(ideContribution))) },
      { pluginView, pluginProvider ->
        PassThruDependenciesModifier.apply(pluginView, pluginProvider) +
          DependencyModification(dependency, CONTENT_MODULE, listOf(requiredContribution)) +
          DependencyModification(optionalDependency, OTHER, listOf(optionalContribution))
      },
      PassThruDependenciesModifier
    )

    assertEquals(
      listOf(
        DependencyModification(
          dependency,
          CONTENT_MODULE,
          contributions = listOf(ideContribution, requiredContribution, optionalContribution)
        )
      ),
      modifier.apply(plugin, ide)
    )
  }
}
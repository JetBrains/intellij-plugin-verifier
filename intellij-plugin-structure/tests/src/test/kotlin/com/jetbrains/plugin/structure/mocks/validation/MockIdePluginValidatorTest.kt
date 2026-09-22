package com.jetbrains.plugin.structure.mocks.validation

import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency.Companion.MandatoryV1Dependency
import com.jetbrains.plugin.structure.mocks.MandatoryV1Dependency
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.SimpleProblemRegistrar
import com.jetbrains.plugin.structure.mocks.idePlugin
import com.jetbrains.plugin.structure.mocks.validation.DuplicateModuleNameProblem.Property.CONTENT_MODULES
import com.jetbrains.plugin.structure.mocks.validation.DuplicateModuleNameProblem.Property.MODULES_DESCRIPTORS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockIdePluginValidatorTest {
  private val validator = MockIdePluginValidator()

  private fun createMockModule(name: String): Module {
    return Module.InlineModule(
      name = name,
      namespace = null,
      actualNamespace = "jetbrains",
      loadingRule = ModuleLoadingRule.REQUIRED,
      textContent = "<idea-plugin></idea-plugin>"
    )
  }

  private fun createMockModuleDescriptor(name: String): ModuleDescriptor {
    val moduleDef = createMockModule(name)
    return ModuleDescriptor(idePlugin(name), moduleDef)
  }

  @Test
  fun `valid plugin with empty properties produces no problems`() {
    val plugin = MockIdePlugin()
    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertTrue(problemRegistrar.problems.isEmpty())
  }

  @Test
  fun `valid plugin with consistent dependencies and modules produces no problems`() {
    val dependsList = listOf(
      MandatoryV1Dependency("com.example.v1.mandatory"),
      DependsPluginDependency("com.example.v1.optional", true)
    )
    val contentModuleDependencies = listOf(
      ContentModuleDependency("com.example.content.module", "jetbrains")
    )
    val pluginMainModuleDependencies = listOf(
      PluginMainModuleDependency("com.example.main.module")
    )

    val dependencies = listOf(
      PluginV1Dependency.Mandatory("com.example.v1.mandatory"),
      PluginV1Dependency.Optional("com.example.v1.optional"),
      ModuleV2Dependency("com.example.content.module"),
      PluginV2Dependency("com.example.main.module")
    )

    val contentModules = listOf(
      createMockModule("mod.one"),
      createMockModule("mod.two")
    )
    val modulesDescriptors = listOf(
      createMockModuleDescriptor("mod.one"),
      createMockModuleDescriptor("mod.two")
    )

    val plugin = MockIdePlugin(
      dependencies = dependencies,
      dependsList = dependsList,
      contentModuleDependencies = contentModuleDependencies,
      pluginMainModuleDependencies = pluginMainModuleDependencies,
      contentModules = contentModules,
      modulesDescriptors = modulesDescriptors
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertTrue(problemRegistrar.problems.isEmpty())
  }

  @Test
  fun `missing dependencies in plugin dependencies list reports DependenciesMismatchProblem`() {
    val plugin = MockIdePlugin(
      dependsList = listOf(DependsPluginDependency("com.example.dep", false)),
      dependencies = emptyList()
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(1, problemRegistrar.problems.size)
    assertTrue(problemRegistrar.problems[0] is DependenciesMismatchProblem)
  }

  @Test
  fun `extra dependency in plugin dependencies list reports DependenciesMismatchProblem`() {
    val plugin = MockIdePlugin(
      dependencies = listOf(PluginV1Dependency.Mandatory("com.example.extra")),
      dependsList = emptyList(),
      contentModuleDependencies = emptyList(),
      pluginMainModuleDependencies = emptyList()
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(1, problemRegistrar.problems.size)
    assertTrue(problemRegistrar.problems[0] is DependenciesMismatchProblem)
  }

  @Test
  fun `dependency ordering mismatch reports DependenciesMismatchProblem`() {
    val dependsList = listOf(DependsPluginDependency("com.example.v1", false))
    val contentModuleDependencies = listOf(ContentModuleDependency("com.example.module", "jetbrains"))
    val pluginMainModuleDependencies = listOf(PluginMainModuleDependency("com.example.main"))

    // Incorrect order: pluginMainModuleDependencies before contentModuleDependencies
    val wrongOrderDependencies = listOf(
      PluginV1Dependency.Mandatory("com.example.v1"),
      PluginV2Dependency("com.example.main"),
      ModuleV2Dependency("com.example.module")
    )

    val plugin = MockIdePlugin(
      dependencies = wrongOrderDependencies,
      dependsList = dependsList,
      contentModuleDependencies = contentModuleDependencies,
      pluginMainModuleDependencies = pluginMainModuleDependencies
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(1, problemRegistrar.problems.size)
    assertTrue(problemRegistrar.problems[0] is DependenciesMismatchProblem)
  }

  @Test
  fun `dependency optional flag mismatch reports DependenciesMismatchProblem`() {
    val plugin = MockIdePlugin(
      dependsList = listOf(DependsPluginDependency("com.example.dep", isOptional = true)),
      dependencies = listOf(PluginV1Dependency.Mandatory("com.example.dep"))
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(1, problemRegistrar.problems.size)
    assertTrue(problemRegistrar.problems[0] is DependenciesMismatchProblem)
  }

  @Test
  fun `module count mismatch with duplicate content module name reports both problems`() {
    // Both have identifier set {"mod.one"}, but count is 1 vs 2 due to duplicate content modules
    val contentModules = listOf(
      createMockModule("mod.one"),
      createMockModule("mod.one")
    )
    val modulesDescriptors = listOf(
      createMockModuleDescriptor("mod.one")
    )

    val plugin = MockIdePlugin(
      contentModules = contentModules,
      modulesDescriptors = modulesDescriptors
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(2, problemRegistrar.problems.size)
    val problem = problemRegistrar.problems.filterIsInstance<ModuleCountMismatchProblem>().single()
    assertEquals(1, problem.descriptorCount)
    assertEquals(2, problem.contentModuleCount)
    assertEquals(
      listOf("mod.one"),
      problemRegistrar.problems.filterIsInstance<DuplicateModuleNameProblem>().single().duplicateNames
    )
    assertEquals(CONTENT_MODULES, problemRegistrar.problems.filterIsInstance<DuplicateModuleNameProblem>().single().property)
  }

  @Test
  fun `duplicate descriptor and content module names report separate problems`() {
    val plugin = MockIdePlugin(
      contentModules = listOf(createMockModule("mod.one"), createMockModule("mod.one")),
      modulesDescriptors = listOf(createMockModuleDescriptor("mod.one"), createMockModuleDescriptor("mod.one"))
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)

    assertEquals(2, problemRegistrar.problems.size)
    val problems = problemRegistrar.problems.filterIsInstance<DuplicateModuleNameProblem>()
    assertEquals(listOf("mod.one"), problems.single { it.property == MODULES_DESCRIPTORS }.duplicateNames)
    assertEquals(listOf("mod.one"), problems.single { it.property == CONTENT_MODULES }.duplicateNames)
  }

  @Test
  fun `duplicate module descriptor names but unique content modules names`() {
    val plugin = MockIdePlugin(
      modulesDescriptors = listOf(createMockModuleDescriptor("mod.one"), createMockModuleDescriptor("mod.one")),
      contentModules = listOf(createMockModule("mod.one"))
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)

    assertEquals(2, problemRegistrar.problems.size)
    val problems = problemRegistrar.problems.filterIsInstance<DuplicateModuleNameProblem>()
    assertEquals(1, problems.size)
    val duplicateContentModuleNameProblem = problems.first()

    assertEquals(MODULES_DESCRIPTORS, duplicateContentModuleNameProblem.property)
    assertEquals(listOf("mod.one"), duplicateContentModuleNameProblem.duplicateNames)
  }


  @Test
  fun `module identifier mismatch with equal count reports ModuleIdentifierMismatchProblem`() {
    val contentModules = listOf(createMockModule("module.alpha"))
    val modulesDescriptors = listOf(createMockModuleDescriptor("module.beta"))

    val plugin = MockIdePlugin(
      contentModules = contentModules,
      modulesDescriptors = modulesDescriptors
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(1, problemRegistrar.problems.size)
    val problem = problemRegistrar.problems[0] as ModuleIdentifierMismatchProblem
    assertEquals(listOf("module.beta"), problem.descriptorIdentifiers)
    assertEquals(listOf("module.alpha"), problem.contentModuleIdentifiers)
  }

  @Test
  fun `multiple simultaneous discrepancies report all corresponding problems`() {
    val contentModules = listOf(
      createMockModule("mod.one"),
      createMockModule("mod.two")
    )
    val modulesDescriptors = listOf(
      createMockModuleDescriptor("mod.three")
    )

    val plugin = MockIdePlugin(
      dependsList = listOf(DependsPluginDependency("com.example.v1", false)),
      contentModules = contentModules,
      modulesDescriptors = modulesDescriptors
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(3, problemRegistrar.problems.size)
    assertTrue(problemRegistrar.problems.any { it is DependenciesMismatchProblem })
    assertTrue(problemRegistrar.problems.any { it is ModuleCountMismatchProblem })
    assertTrue(problemRegistrar.problems.any { it is ModuleIdentifierMismatchProblem })
  }

  @Test
  fun `validate IdePlugin overload handles valid and invalid plugins`() {
    val idePlugin: IdePlugin = MockIdePlugin(
      dependsList = listOf(DependsPluginDependency("com.example.dep", false)),
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(idePlugin, problemRegistrar)
    assertEquals(1, problemRegistrar.problems.size)
    assertTrue(problemRegistrar.problems[0] is DependenciesMismatchProblem)
  }
}

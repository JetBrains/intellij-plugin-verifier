package com.jetbrains.plugin.structure.mocks.validation

import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.DependsPluginDependency.Companion.MandatoryV1Dependency
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.SimpleProblemRegistrar
import com.jetbrains.plugin.structure.mocks.idePlugin
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

    val contentModules = listOf(
      createMockModule("mod.one"),
      createMockModule("mod.two")
    )
    val modulesDescriptors = listOf(
      createMockModuleDescriptor("mod.one"),
      createMockModuleDescriptor("mod.two")
    )

    val plugin = MockIdePlugin(
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
  fun `module count mismatch with same unique identifiers reports ModuleCountMismatchProblem`() {
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
    assertEquals(1, problemRegistrar.problems.size)
    val problem = problemRegistrar.problems[0] as ModuleCountMismatchProblem
    assertEquals(1, problem.descriptorCount)
    assertEquals(2, problem.contentModuleCount)
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
    assertEquals(setOf("module.beta"), problem.descriptorIdentifiers)
    assertEquals(setOf("module.alpha"), problem.contentModuleIdentifiers)
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
      contentModules = contentModules,
      modulesDescriptors = modulesDescriptors
    )

    val problemRegistrar = SimpleProblemRegistrar()
    validator.validate(plugin, problemRegistrar)
    assertEquals(2, problemRegistrar.problems.size)
    assertTrue(problemRegistrar.problems.any { it is ModuleCountMismatchProblem })
    assertTrue(problemRegistrar.problems.any { it is ModuleIdentifierMismatchProblem })
  }
}

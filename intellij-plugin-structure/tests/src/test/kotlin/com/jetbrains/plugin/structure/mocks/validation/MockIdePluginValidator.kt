package com.jetbrains.plugin.structure.mocks.validation

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.SimpleProblemRegistrar
import com.jetbrains.plugin.structure.mocks.validation.DuplicateModuleNameProblem.Property.CONTENT_MODULES
import com.jetbrains.plugin.structure.mocks.validation.DuplicateModuleNameProblem.Property.MODULES_DESCRIPTORS

class MockIdePluginValidator {
  fun validate(plugin: MockIdePlugin, problemRegistrar: ProblemRegistrar) {
    validate(plugin as IdePlugin, problemRegistrar)
  }

  fun validate(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    validateDependencies(plugin, problemRegistrar)
    validateModuleCounts(plugin, problemRegistrar)
    validateModuleIdentifiers(plugin, problemRegistrar)
    validateDuplicateModuleNames(plugin, problemRegistrar)
  }

  @Suppress("DEPRECATION")
  private fun validateDependencies(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    val expectedDependencies = plugin.dependsList.map { it.asPluginDependency() } +
      plugin.contentModuleDependencies.map { ModuleV2Dependency(it.moduleName) } +
      plugin.pluginMainModuleDependencies.map { PluginV2Dependency(it.pluginId) }

    if (plugin.dependencies != expectedDependencies) {
      problemRegistrar.registerProblem(DependenciesMismatchProblem())
    }
  }

  private fun validateModuleCounts(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    if (plugin.modulesDescriptors.size != plugin.contentModules.size) {
      problemRegistrar.registerProblem(ModuleCountMismatchProblem(plugin.modulesDescriptors.size, plugin.contentModules.size))
    }
  }

  private fun validateModuleIdentifiers(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    val descriptorIdentifiers = plugin.modulesDescriptors.map { it.name }.distinct().sorted()
    val contentModuleIdentifiers = plugin.contentModules.map { it.name }.distinct().sorted()
    if (descriptorIdentifiers != contentModuleIdentifiers) {
      problemRegistrar.registerProblem(ModuleIdentifierMismatchProblem(descriptorIdentifiers, contentModuleIdentifiers))
    }
  }

  private fun validateDuplicateModuleNames(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    val duplicateDescriptorNames = plugin.modulesDescriptors.map { it.name }.duplicates()
    val duplicateContentModuleNames = plugin.contentModules.map { it.name }.duplicates()
    if (duplicateDescriptorNames.isNotEmpty()) {
      problemRegistrar.registerProblem(DuplicateModuleNameProblem(duplicateDescriptorNames, MODULES_DESCRIPTORS))
    }
    if (duplicateContentModuleNames.isNotEmpty()) {
      problemRegistrar.registerProblem(DuplicateModuleNameProblem(duplicateContentModuleNames, CONTENT_MODULES))
    }
  }

  private fun List<String>.duplicates(): List<String> = groupingBy { it }
    .eachCount()
    .filterValues { it > 1 }
    .keys
    .sorted()

  companion object {
    fun MockIdePlugin.assertValid(): MockIdePlugin {
      val problems = SimpleProblemRegistrar()
      MockIdePluginValidator().validate(this, problems)
      if (problems.isNotEmpty()) {
        throw IllegalStateException(problems.joinToString())
      }
      return this
    }
  }
}

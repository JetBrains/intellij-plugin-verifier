package com.jetbrains.plugin.structure.mocks.validation

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.verifiers.ProblemRegistrar
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.SimpleProblemRegistrar

class MockIdePluginValidator {
  fun validate(plugin: MockIdePlugin, problemRegistrar: ProblemRegistrar) {
    validate(plugin as IdePlugin, problemRegistrar)
  }

  fun validate(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    validateModuleCounts(plugin, problemRegistrar)
    validateModuleIdentifiers(plugin, problemRegistrar)
  }

  private fun validateModuleCounts(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    if (plugin.modulesDescriptors.size != plugin.contentModules.size) {
      problemRegistrar.registerProblem(ModuleCountMismatchProblem(plugin.modulesDescriptors.size, plugin.contentModules.size))
    }
  }

  private fun validateModuleIdentifiers(plugin: IdePlugin, problemRegistrar: ProblemRegistrar) {
    val descriptorIdentifiers = plugin.modulesDescriptors.map { it.name }.sorted()
    val contentModuleIdentifiers = plugin.contentModules.map { it.name }.sorted()
    if (descriptorIdentifiers != contentModuleIdentifiers) {
      problemRegistrar.registerProblem(ModuleIdentifierMismatchProblem(descriptorIdentifiers, contentModuleIdentifiers))
    }
  }

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

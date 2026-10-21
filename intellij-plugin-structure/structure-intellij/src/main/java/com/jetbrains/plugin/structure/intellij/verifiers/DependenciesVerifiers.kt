package com.jetbrains.plugin.structure.intellij.verifiers

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.ReusedDescriptorInMultipleDependencies
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.plugin.ValidatableDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ValidatableDescriptor.DependencyView
import com.jetbrains.plugin.structure.intellij.plugin.toDependencyView

/**
 * Validates if each dependency in `<depends>` provides a unique `config-file`, if such file is declared.
 *
 * Multiple dependencies specified in the same plugin descriptor cannot redeclare same `config-file`.
 */
class ReusedDescriptorVerifier(private val descriptorPath: String? = null) {
  fun verify(dependencies: Collection<PluginDependencyBean>, problemConsumer: (PluginProblem) -> Unit) =
    verifyDependencies(dependencies.map { it.toDependencyView() }, problemConsumer)

  fun verify(descriptor: ValidatableDescriptor, problemConsumer: (PluginProblem) -> Unit) =
    verifyDependencies(descriptor.dependencies, problemConsumer)

  private fun verifyDependencies(dependencies: List<DependencyView>, problemConsumer: (PluginProblem) -> Unit) {
    dependencies.groupBy { it.configFile }
            .filterKeys { it != null }
            .filterValues { it.size > 1 }
            .forEach { (configFile, dependencies) ->
              val dependencyIdentifiers = dependencies.map { it.pluginId.orEmpty() }
              problemConsumer(ReusedDescriptorInMultipleDependencies(descriptorPath, configFile!!, dependencyIdentifiers))
            }
  }
}
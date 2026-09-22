package com.jetbrains.plugin.structure.mocks.validation

import com.jetbrains.plugin.structure.base.problems.PluginProblem

open class DependentPropertyProblem(
  override val message: String,
  override val level: Level = Level.ERROR
) : PluginProblem()

class DependenciesMismatchProblem(
  message: String = "The 'dependencies' property is inconsistent with 'dependsList', 'pluginMainModuleDependencies', and 'contentModuleDependencies'."
) : DependentPropertyProblem(message)

class ModuleCountMismatchProblem(
  val descriptorCount: Int? = null,
  val contentModuleCount: Int? = null,
  message: String = if (descriptorCount != null && contentModuleCount != null) {
    "The number of 'modulesDescriptors' ($descriptorCount) does not match the number of 'contentModules' ($contentModuleCount)."
  } else {
    "The number of 'modulesDescriptors' does not match the number of 'contentModules'."
  }
) : DependentPropertyProblem(message)

class ModuleIdentifierMismatchProblem(
  val descriptorIdentifiers: Set<String>? = null,
  val contentModuleIdentifiers: Set<String>? = null,
  message: String = if (descriptorIdentifiers != null && contentModuleIdentifiers != null) {
    "The module identifiers in 'modulesDescriptors' ($descriptorIdentifiers) do not match the identifiers in 'contentModules' ($contentModuleIdentifiers)."
  } else {
    "The module identifiers in 'modulesDescriptors' do not match the identifiers in 'contentModules'."
  }
) : DependentPropertyProblem(message)

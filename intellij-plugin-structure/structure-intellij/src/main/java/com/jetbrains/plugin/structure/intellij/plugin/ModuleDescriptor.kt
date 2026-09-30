/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a plugin content module with metadata and type-safe parsed content module descriptor.
 */
sealed class ModuleDescriptor {
  /**
   * Content module descriptor in a resolved type-safe form.
   */
  abstract val module: IdePlugin

  /**
   * Content module metadata such as loading rules, namespaces and path to descriptor.
   */
  abstract val moduleDefinition: Module

  /**
   * Resolved dependencies of the content module.
   * These dependencies are different from the dependencies declared in the module descriptor.
   * They are filtered for duplicates in occurring in the main plugin module (the `plugin.xml`).
   * Additionally, they might have specific subtypes, such as [InlineDeclaredModuleV2Dependency] or similar.
   */
  abstract val resolvedDependencies: List<PluginDependency>

  val name get() = moduleDefinition.name

  companion object {
    fun of(
      module: IdePlugin,
      moduleDefinition: Module,
      resolvedDependencies: List<PluginDependency> = emptyList()
    ): ModuleDescriptor = when (moduleDefinition) {
      is Module.InlineModule -> InlineModuleDescriptor(module, moduleDefinition, resolvedDependencies)
      is Module.FileBasedModule -> FileBasedModuleDescriptor(module, moduleDefinition, resolvedDependencies)
    }
  }
}

data class InlineModuleDescriptor(
  override val module: IdePlugin,
  override val moduleDefinition: Module.InlineModule,
  override val resolvedDependencies: List<PluginDependency>
) : ModuleDescriptor()

data class FileBasedModuleDescriptor(
  override val module: IdePlugin,
  override val moduleDefinition: Module.FileBasedModule,
  override val resolvedDependencies: List<PluginDependency>
) : ModuleDescriptor()

val ModuleDescriptor.dependencies: List<PluginDependency> get() = module.dependencies
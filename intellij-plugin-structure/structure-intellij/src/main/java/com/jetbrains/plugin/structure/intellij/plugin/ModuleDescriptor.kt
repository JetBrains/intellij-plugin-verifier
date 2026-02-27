/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Represents a plugin content module with metadata and type-safe parsed content module descriptor.
 * @param module content module descriptor in a resolved type-safe form.
 * @param moduleDefinition content module metadata such as loading rules, namespaces and path to descriptor.
 */
data class ModuleDescriptor(
  val module: IdePlugin,
  val moduleDefinition: Module
) {
  val name = moduleDefinition.name

  companion object {
    fun of(
      module: IdePlugin,
      moduleDefinition: Module
    ): ModuleDescriptor =
      ModuleDescriptor(module, moduleDefinition)
  }
}

val ModuleDescriptor.dependencies: List<PluginDependency> get() = module.dependencies
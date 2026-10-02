/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.DependencyModificationReason.*

/**
 * Constructs dependency modifications for the given plugin by individually parsing
 * V1 dependencies in `<depends>` and V2 dependencies in `<dependencies>`.
 *
 * @param includeContentModuleDependencies whether to also collect dependencies declared by the
 *   plugin's resolved content modules.
 */
class DefaultDependencyContributor(private val includeContentModuleDependencies: Boolean) : DependenciesModifier {
  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification> {
    val dependencies = if (includeContentModuleDependencies) {
      plugin.reconstructAllDependencies()
    } else {
      plugin.reconstructDependencies()
    }
    return dependencies.map { dependency ->
      val modification = toDependencyModification(dependency)
      if (includeContentModuleDependencies) {
        modification.copy(contributions = plugin.getDependencyContributions(dependency))
      } else modification
    }
  }

  private fun toDependencyModification(dependency: PluginDependency): DependencyModification {
    val reason = when (dependency) {
      is PluginV1Dependency -> when (dependency) {
        is PluginV1Dependency.Mandatory -> PLUGIN
        is PluginV1Dependency.Optional -> PLUGIN
      }

      is InlineDeclaredModuleV2Dependency -> when (dependency) {
        is InlineDeclaredModuleV2Dependency.Plugin -> PLUGIN
        is InlineDeclaredModuleV2Dependency.Module -> CONTENT_MODULE
      }

      is PluginV2Dependency -> PLUGIN
      is ModuleV2Dependency -> CONTENT_MODULE
      is PluginDependencyImpl -> PLUGIN
      else -> OTHER
    }
    return DependencyModification(dependency, reason)
  }
}
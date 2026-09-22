/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.DependencyModificationReason.*

/**
 * Constructs dependency modifications for the given plugin by individually parsing
 * V1 dependencies in `<depends>` and V2 dependencies in `<dependencies>`.
 *
 * It intentionally ignores the [IdePlugin.dependencies] as this property is deprecated.
 */
class DefaultDependencyContributor(private val includeContentModuleDependencies: Boolean, private val fallbackToAggregatedDependencies: Boolean = false) : DependenciesModifier {
  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification> = buildList {
    this += resolve(plugin)
    if (includeContentModuleDependencies) {
      this += resolveContentModules(plugin)
    }
    if (this.isEmpty() && fallbackToAggregatedDependencies) {
      this += fallbackPlugin(plugin)
    }
  }

  private fun resolve(plugin: IdePlugin): List<DependencyModification> = buildList {
    this += plugin.dependsList.map {
      DependencyModification(it.asPluginDependency(), PLUGIN)
    }
    this += plugin.pluginMainModuleDependencies.map {
      DependencyModification(it.asPluginDependency(), PLUGIN)
    }
    this += plugin.contentModuleDependencies.map {
      DependencyModification(it.asPluginDependency(), CONTENT_MODULE)
    }
  }

  private fun resolveContentModules(plugin: IdePlugin): List<DependencyModification> {
    return plugin.modulesDescriptors.flatMap {
      resolve(it.module)
    }
  }

  private fun fallbackPlugin(plugin: IdePlugin): List<DependencyModification> = buildList {
    this += fallback(plugin)
    if (includeContentModuleDependencies) {
      this += fallbackContentModules(plugin)
    }
  }

  private fun fallback(plugin: IdePlugin): List<DependencyModification> {
    return plugin.dependencies.map { dependency ->
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
      DependencyModification(dependency, reason)
    }
  }

  private fun fallbackContentModules(plugin: IdePlugin): List<DependencyModification> {
    return plugin.modulesDescriptors.flatMap {
      fallback(it.module)
    }
  }

}
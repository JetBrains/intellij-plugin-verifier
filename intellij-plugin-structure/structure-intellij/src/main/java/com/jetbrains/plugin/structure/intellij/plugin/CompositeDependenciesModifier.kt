/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Composes multiple [DependenciesModifier]s into a single modifier.
 *
 * Each modifier is applied in sequence, with each modifier receiving the output
 * of the previous modifier. This allows for chaining multiple dependency
 * contribution rules.
 *
 * Example:
 * ```
 * val composite = CompositeDependenciesModifier(
 *   corePluginContributor,      // Adds core plugin dependency
 *   legacyPluginContributor     // Adds Java module for legacy plugins
 * )
 * ```
 */
class CompositeDependenciesModifier(
  private val modifiers: List<DependenciesModifier>
) : DependenciesModifier {

  constructor(vararg modifiers: DependenciesModifier) : this(modifiers.toList())

  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification> {
    if (modifiers.isEmpty()) {
      return getInitialDependencyModifications(plugin)
    }

    var currentDependencyModifications = getInitialDependencyModifications(plugin)
    for (modifier in modifiers) {
      val pluginView = DependencyModifiedPluginView(plugin, currentDependencyModifications)
      currentDependencyModifications = mergeDependencyModifications(
        currentDependencyModifications,
        modifier.apply(pluginView, pluginProvider)
      )
    }

    return currentDependencyModifications
  }

  private fun getInitialDependencyModifications(plugin: IdePlugin): List<DependencyModification> =
    plugin.dependencies.map { it to it.inferredModificationReason() }

  private fun PluginDependency.inferredModificationReason(): DependencyModificationReason {
    return if (this is ModuleV2Dependency) {
      DependencyModificationReason.CONTENT_MODULE
    } else {
      DependencyModificationReason.PLUGIN
    }
  }

  private fun mergeDependencyModifications(
    current: List<DependencyModification>,
    modified: List<DependencyModification>
  ): List<DependencyModification> {
    val currentByDependencyId = current.withHighestPriorityReasons().associateBy { it.first.id }
    return modified
      .map { dependencyModification ->
        currentByDependencyId[dependencyModification.first.id]
          ?.withHighestPriorityReason(dependencyModification)
          ?: dependencyModification
      }
      .withHighestPriorityReasons()
  }

  private fun List<DependencyModification>.withHighestPriorityReasons(): List<DependencyModification> {
    val merged = linkedMapOf<String, DependencyModification>()
    for (dependencyModification in this) {
      val id = dependencyModification.first.id
      val previous = merged[id]
      merged[id] = previous?.withHighestPriorityReason(dependencyModification) ?: dependencyModification
    }
    return merged.values.toList()
  }

  private fun DependencyModification.withHighestPriorityReason(other: DependencyModification): DependencyModification {
    return if (second >= other.second) this else other
  }

  /**
   * A lightweight wrapper that presents a plugin with modified dependencies
   * without copying the entire plugin object.
   */
  private class DependencyModifiedPluginView(
    private val delegate: IdePlugin,
    override val dependencyModifications: List<DependencyModification>
  ) : IdePlugin by delegate, DependencyModificationsAware {
    @Deprecated("contains mixed dependencies, including ones that belong to content modules; see dependsList, pluginMainModuleDependencies, contentModuleDependencies")
    override val dependencies: List<PluginDependency> = dependencyModifications.map { it.first }
  }
}
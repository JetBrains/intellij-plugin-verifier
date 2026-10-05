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
 * Within each modifier's output, duplicate dependency IDs keep their first-occurrence order. The first
 * mandatory effective dependency is selected, or the first dependency if all are optional. Reasons are
 * merged independently using the highest priority, and only the output's contributions are combined,
 * deduplicated by source, dependency ID and optionality. Each contribution retains its own dependency.
 *
 * Between modifiers, the returned IDs and contribution lists replace the preceding stage's result,
 * including explicitly empty contribution lists. For retained IDs, the highest-priority reason is preserved.
 * The returned effective dependency is preferred unless it would replace a mandatory dependency with an
 * optional one. Equal-optionality replacements are accepted regardless of reason priority.
 *
 * To preserve sources, carry existing [DependencyModification]s forward, optionally using [DependencyModification.copy].
 * Reconstructing modifications from raw dependencies replaces sources with the default main-only contributions.
 * An empty composite returns the initial modifications unchanged.
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
    plugin.getDependencyModifications()

  private fun mergeDependencyModifications(
    current: List<DependencyModification>,
    modified: List<DependencyModification>
  ): List<DependencyModification> {
    val currentByDependencyId = current.withHighestPriorityReasons().associateBy { it.dependency.id }
    return modified
      .withHighestPriorityReasons()
      .map { dependencyModification ->
        currentByDependencyId[dependencyModification.dependency.id]
          ?.withHighestPriorityReason(dependencyModification)
          ?: dependencyModification
      }
  }

  /**
   * Normalizes duplicate dependency IDs within one list, preserving their first-occurrence order.
   *
   * Selects the first mandatory effective dependency, or the first dependency if all are optional,
   * independently of the highest-priority reason. Combines only this list's contributions, deduplicated
   * by source, dependency ID and optionality, while preserving each contribution's own dependency.
   * Does not reconcile contributions with a preceding stage's result.
   */
  private fun List<DependencyModification>.withHighestPriorityReasons(): List<DependencyModification> {
    return groupBy { it.dependency.id }.map { (_, duplicates) ->
      val preferred = duplicates.firstOrNull { !it.dependency.isOptional } ?: duplicates.first()
      preferred.copy(
        reason = duplicates.maxOf { it.reason },
        contributions = duplicates.flatMap { it.contributions }.distinctContributions()
      )
    }
  }

  private fun DependencyModification.withHighestPriorityReason(other: DependencyModification): DependencyModification {
    return other.copy(
      dependency = if (!dependency.isOptional && other.dependency.isOptional) dependency else other.dependency,
      reason = maxOf(reason, other.reason)
    )
  }

  private fun List<DependencyContribution>.distinctContributions(): List<DependencyContribution> =
    distinctBy {
      val contributingContentModule = when (it) {
        is ContentModuleDependencyContribution -> it.contributingContentModule
        is PluginMainModuleDependencyContribution -> null
      }
      Triple(contributingContentModule, it.dependency.id, it.dependency.isOptional)
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
    override val dependencies: List<PluginDependency> = dependencyModifications.map { it.dependency }
  }
}
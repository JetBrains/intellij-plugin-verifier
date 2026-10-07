/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Composes multiple [DependenciesModifier]s into a single modifier.
 *
 * Modifiers are applied in order. Each modifier receives the previous stage's result through the plugin view:
 * its dependency modifications (including reasons and contributions) and the corresponding effective dependencies.
 *
 * Each modifier returns the complete resulting list, which replaces the previous stage's result entirely.
 * Omitted dependency IDs and contributions are removed. The last modifier wins: its output is the result,
 * including any optionality change in either direction (optional to mandatory or mandatory to optional).
 *
 * Each modifier's output is deduplicated by dependency ID before being passed on: the last entry for an ID wins
 * and keeps its position, while earlier entries with the same ID are dropped. No merging takes place between
 * stages or between duplicates; dependency, reason and contributions are taken from the winning entry only.
 *
 * [DependencyModification.reason] is carried through as returned by each modifier and serves debugging purposes only.
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

  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification> =
    modifiers.fold(plugin.getDependencyModifications()) { current, modifier ->
      modifier.apply(DependencyModifiedPluginView(plugin, current), pluginProvider).lastWinsByDependencyId()
    }

  /**
   * Keeps only the last [DependencyModification] for each dependency ID, at the position of that last occurrence.
   */
  private fun List<DependencyModification>.lastWinsByDependencyId(): List<DependencyModification> =
    asReversed().distinctBy { it.dependency.id }.asReversed()

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
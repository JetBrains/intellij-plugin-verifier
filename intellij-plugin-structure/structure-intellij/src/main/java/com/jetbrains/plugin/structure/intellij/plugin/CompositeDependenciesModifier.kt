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

  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<PluginDependency> {
    if (modifiers.isEmpty()) {
      return plugin.dependencies
    }

    var currentDependencies = plugin.dependencies
    for (modifier in modifiers) {
      val pluginView = DependencyModifiedPluginView(plugin, currentDependencies)
      currentDependencies = modifier.apply(pluginView, pluginProvider)
    }

    return currentDependencies
  }

  /**
   * A lightweight wrapper that presents a plugin with modified dependencies
   * without copying the entire plugin object.
   */
  private class DependencyModifiedPluginView(
    private val delegate: IdePlugin,
    @Deprecated("contains mixed dependencies, including ones that belong to content modules; see dependsList, pluginMainModuleDependencies, contentModuleDependencies")
    override val dependencies: List<PluginDependency>
  ) : IdePlugin by delegate
}
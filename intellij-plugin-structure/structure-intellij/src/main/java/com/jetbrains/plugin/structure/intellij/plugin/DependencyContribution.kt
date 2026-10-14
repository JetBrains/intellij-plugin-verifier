/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Records the source of one contribution to a [DependencyModification].
 *
 * The source is the plugin's main descriptor or a named content module that declares the dependency,
 * not the plugin or module being depended on. An implicit dependency added by a modifier can also have
 * a main-plugin-level contribution without a corresponding descriptor declaration.
 *
 * [ContentModuleDependencyContribution] identifies a named content module with a non-null source name.
 * [PluginMainModuleDependencyContribution] represents a main-plugin-level source without a module-name
 * property; it is also used for implicit dependencies and as fallback provenance when no declaration is found.
 *
 * [dependency] retains the dependency details at that source, including optionality. These can differ
 * from [DependencyModification.dependency] when several sources contribute to the same dependency ID.
 * Contributions are provenance metadata, not additional dependencies to add to the result.
 *
 * Example: the content module `example.editor` depends on `example.language.api`:
 * ```
 * val contribution: DependencyContribution = ContentModuleDependencyContribution(
 *   sourceModuleName = "example.editor",
 *   dependency = ModuleV2Dependency("example.language.api")
 * )
 * ```
 *
 * @property dependency dependency supplied by this source, preserving its own declaration details.
 */
sealed class DependencyContribution {
  abstract val dependency: PluginDependency
}

/**
 * A dependency contributed by a named content module.
 *
 * @property contributingContentModule non-null name of the contributing content module, not the dependency's target.
 * @property dependency dependency declared by that module, preserving its own optionality.
 */
data class ContentModuleDependencyContribution(
  val contributingContentModule: String,
  override val dependency: PluginDependency
) : DependencyContribution()

/**
 * A main-plugin-level contribution without a named content-module source.
 *
 * Used for dependencies declared in the main descriptor, implicit dependencies added by modifiers,
 * and fallback provenance when no matching declaration is available. It does not imply an explicit declaration.
 * The dependency's target can be either a plugin or a module.
 *
 * Example: an implicit dependency on the IDE's core plugin:
 * ```
 * val contribution = PluginMainModuleDependencyContributon(PluginV1Dependency.Mandatory("com.intellij"))
 * ```
 *
 * @property dependency dependency supplied at the main-plugin level, preserving its own optionality.
 */
data class PluginMainModuleDependencyContribution(
  override val dependency: PluginDependency
) : DependencyContribution()
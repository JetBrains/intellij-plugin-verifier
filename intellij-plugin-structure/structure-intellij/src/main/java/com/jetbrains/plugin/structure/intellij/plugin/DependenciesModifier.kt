/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.DependencyModificationReason.CONTENT_MODULE
import com.jetbrains.plugin.structure.intellij.plugin.DependencyModificationReason.PLUGIN

enum class DependencyModificationReason {
  OTHER,
  IDE,
  PLUGIN,
  CONTENT_MODULE
}



/**
 * A resulting dependency together with its reason and contribution sources.
 *
 * Despite the name, this is not an add/remove command: [DependenciesModifier.apply] returns the complete
 * resulting dependency list, including unchanged dependencies. Creating this value does not mutate the plugin.
 *
 * [dependency] is the effective dependency exposed to downstream modifiers. [reason] explains why it is
 * present, rather than what operation was performed or which module declared it. For example, contributors
 * use [DependencyModificationReason.PLUGIN] for plugin dependencies, [DependencyModificationReason.CONTENT_MODULE]
 * for module dependencies, and [DependencyModificationReason.IDE] for implicit IDE dependencies.
 *
 * [contributions] records the source-level dependencies behind this effective dependency. By default it
 * contains one [PluginMainModuleDependencyContribution] using [dependency]; supplying an explicit list replaces
 * that default. Multiple sources can contribute the same dependency ID while retaining different optionality.
 *
 * Example: a mandatory main-plugin dependency is also declared optionally by a content module:
 * ```
 * val dependency = ModuleV2Dependency("example.language.api")
 * val mainOnly = DependencyModification(dependency, DependencyModificationReason.CONTENT_MODULE)
 * // mainOnly.contributions contains PluginMainModuleDependencyContributon(dependency).
 * val shared = mainOnly.copy(
 *   contributions = listOf(
 *     PluginMainModuleDependencyContributon(dependency),
 *     ContentModuleDependencyContribution("example.editor", dependency.asOptional())
 *   )
 * )
 * // shared.dependency remains mandatory; the module's contribution remains optional.
 * ```
 *
 * @property dependency effective dependency retained in the modifier's result.
 * @property reason category explaining why the dependency is present.
 * @property contributions provenance of the dependency, retaining each source's dependency details.
 */
data class DependencyModification(
  val dependency: PluginDependency,
  val reason: DependencyModificationReason,
  val contributions: List<DependencyContribution> = listOf(PluginMainModuleDependencyContribution(dependency))
)

fun interface DependenciesModifier {
  fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification>
}

/**
 * Marks an [IdePlugin] view whose dependencies already have [DependencyModificationReason] metadata attached.
 *
 * [IdePlugin.dependencies] exposes only raw [PluginDependency]s, so consumers would otherwise have to infer reasons
 * again and could lose explicit modifier-provided reasons such as [DependencyModificationReason.IDE].
 * [getDependencyModifications] uses this interface to preserve existing [DependencyModification]s while chaining
 * [DependenciesModifier] instances.
 */
internal interface DependencyModificationsAware {
  val dependencyModifications: List<DependencyModification>
}

internal fun IdePlugin.getDependencyModifications(): List<DependencyModification> {
  return if (this is DependencyModificationsAware) {
    dependencyModifications
  } else {
    dependencies.withInferredModificationReasons().map { modification ->
      modification.copy(contributions = getDependencyContributions(modification.dependency))
    }
  }
}

internal fun IdePlugin.getDependencyContributions(dependency: PluginDependency): List<DependencyContribution> {
  if (modulesDescriptors.isEmpty()) return listOf(PluginMainModuleDependencyContribution(dependency))
  val declarations = reconstructDependencies().filter { it.id == dependency.id }
    .map { PluginMainModuleDependencyContribution(it) } + modulesDescriptors.flatMap { descriptor ->
    descriptor.declaredDependencies.filter { it.id == dependency.id }
      .map { ContentModuleDependencyContribution(descriptor.name, it) }
  }
  return declarations.ifEmpty { listOf(PluginMainModuleDependencyContribution(dependency)) }
}

internal fun List<PluginDependency>.withInferredModificationReasons(): List<DependencyModification> = map {
  DependencyModification(
    it, when (it) {
    is ModuleV2Dependency, is InlineDeclaredModuleV2Dependency.Module -> CONTENT_MODULE
    else -> PLUGIN
  }
  )
}

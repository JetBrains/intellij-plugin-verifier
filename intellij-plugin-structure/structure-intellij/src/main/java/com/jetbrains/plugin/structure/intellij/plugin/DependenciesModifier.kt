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

data class DependencyContribution(val sourceModuleName: String?, val dependency: PluginDependency)

data class DependencyModification(
  val dependency: PluginDependency,
  val reason: DependencyModificationReason,
  val contributions: List<DependencyContribution> = listOf(DependencyContribution(null, dependency))
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
  if (modulesDescriptors.isEmpty()) return listOf(DependencyContribution(null, dependency))
  val declarations = reconstructDependencies().filter { it.id == dependency.id }
    .map { DependencyContribution(null, it) } + modulesDescriptors.flatMap { descriptor ->
    descriptor.declaredDependencies.filter { it.id == dependency.id }
      .map { DependencyContribution(descriptor.name, it) }
  }
  return declarations.ifEmpty { listOf(DependencyContribution(null, dependency)) }
}

internal fun List<PluginDependency>.withInferredModificationReasons(): List<DependencyModification> = map {
  DependencyModification(
    it, when (it) {
    is ModuleV2Dependency -> CONTENT_MODULE
    else -> PLUGIN
  }
  )
}

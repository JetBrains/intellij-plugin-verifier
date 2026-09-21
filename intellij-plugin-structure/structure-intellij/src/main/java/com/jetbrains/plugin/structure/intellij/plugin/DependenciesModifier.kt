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

typealias DependencyModification = Pair<PluginDependency, DependencyModificationReason>

fun interface DependenciesModifier {
  fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<DependencyModification>
}

/**
 * Marks an [IdePlugin] view whose dependencies already have [DependencyModificationReason] metadata attached.
 *
 * [IdePlugin.dependencies] exposes only raw [PluginDependency]s, so consumers would otherwise have to infer reasons
 * again and could lose explicit modifier-provided reasons such as [DependencyModificationReason.IDE].
 * [getDependencyModifications] uses this interface to preserve existing [DependencyModification] pairs while chaining
 * [DependenciesModifier] instances.
 */
internal interface DependencyModificationsAware {
  val dependencyModifications: List<DependencyModification>
}

internal fun IdePlugin.getDependencyModifications(): List<DependencyModification> {
  return (this as? DependencyModificationsAware)?.dependencyModifications
    ?: dependencies.withInferredModificationReasons()
}

internal fun List<PluginDependency>.withInferredModificationReasons(): List<DependencyModification> = associateWith {
  when (it) {
    is ModuleV2Dependency -> CONTENT_MODULE
    else -> PLUGIN
  }
}.toList()

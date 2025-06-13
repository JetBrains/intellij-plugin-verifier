/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.BundledPluginClassesFinder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.StructurallyValidated
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyOrigin.Bundled
import com.jetbrains.pluginverifier.plugin.PluginDetails
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.dependency.DependencyPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.local.LocalPluginInfo

internal fun DependencyFinder.Result.FoundPlugin.getDetails(ide: Ide): PluginDetails {
  return if (origin == Bundled) {
    getBundledPluginDetails(ide, plugin)
  } else {
    getNonBundledDependencyDetails(plugin)
  }
}

internal fun DependencyFinder.Result.DetailsProvided.getDetails(): PluginDetails? {
  return (pluginDetailsCacheResult as? PluginDetailsCache.Result.Provided)?.pluginDetails
}

internal fun getBundledPluginDetails(ide: Ide, plugin: IdePlugin): PluginDetails {
  val pluginWarnings =
    (if (plugin is StructurallyValidated) plugin.problems else emptyList()).filter { it.level == PluginProblem.Level.WARNING }
  return PluginDetails(
    BundledPluginInfo(ide.version, plugin), plugin, pluginWarnings,
    BundledPluginClassesFinder.findPluginClasses(plugin, additionalKeys = listOf(CompileServerExtensionKey)), null
  )
}

internal fun getNonBundledDependencyDetails(plugin: IdePlugin): PluginDetails {
  val pluginWarnings =
    (if (plugin is StructurallyValidated) plugin.problems else emptyList()).filter { it.level == PluginProblem.Level.WARNING }
  return PluginDetails(
    DependencyPluginInfo(LocalPluginInfo(plugin)), plugin, pluginWarnings,
    BundledPluginClassesFinder.findPluginClasses(plugin, additionalKeys = listOf(CompileServerExtensionKey)), null
  )
}




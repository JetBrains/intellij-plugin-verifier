/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.DependenciesModifier
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency

private const val CORE_IDE_PLUGIN_ID = "com.intellij"

/**
 * Contributes a _Core Plugin_ (`com.intellij`) as an implicit dependency to all plugins.
 *
 * According to the IntelliJ plugin model, the `coreLoader` is always automatically added
 * as the last parent classloader for all plugin modules. This modifier emulates that behavior
 * by adding the core plugin as an implicit dependency, ensuring that core platform classes
 * (like `com.intellij.notification.Notification`) are available to all plugins.
 *
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/modular-plugins.html#class-loaders">Plugin Model documentation</a>
 */
class CorePluginDependencyContributor(private val ide: PluginProvider) : DependenciesModifier {

  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<PluginDependency> {
    // Core plugin doesn't depend on itself
    if (plugin.pluginId == CORE_IDE_PLUGIN_ID) {
      return plugin.dependencies
    }

    // Check if plugin already has a dependency on the core plugin
    if (plugin.dependencies.any { it.id == CORE_IDE_PLUGIN_ID }) {
      return plugin.dependencies
    }

    // Add core plugin as implicit dependency if it exists in the IDE
    val corePlugin = ide.findPluginById(CORE_IDE_PLUGIN_ID)
    if (corePlugin != null) {
      return plugin.dependencies + PluginV1Dependency.Mandatory(CORE_IDE_PLUGIN_ID)
    }

    return plugin.dependencies
  }
}
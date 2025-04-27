/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies.legacy

import com.jetbrains.plugin.structure.intellij.plugin.DependenciesModifier
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider

private const val ALL_MODULES_ID = "com.intellij.modules.all"
private const val JAVA_MODULE_ID = "com.intellij.modules.java"
private const val UNKNOWN_PLUGIN_ID = "Unknown Plugin"

/**
 * Contributes a _Java Module_ as a dependency to legacy plugin.
 *
 * If a plugin doesn't declare any dependencies in its `plugin.xml` file, or if it declares dependencies only on
  * other plugins but not modules, it is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA.
 */
class LegacyPluginDependencyContributor: DependenciesModifier {
  override fun apply(plugin: IdePlugin, pluginProvider: PluginProvider): List<PluginDependency> {
    if (pluginProvider.findPluginByModule(ALL_MODULES_ID) == null) {
      return plugin.dependencies
    }
    val isLegacyPlugin = plugin.dependencies.none { it.isModule }
    val isNonBundledPlugin = plugin.isNonBundled(pluginProvider)
    if (isNonBundledPlugin || isLegacyPlugin) {
      val javaModule = pluginProvider.findPluginByModule(JAVA_MODULE_ID)
      if (javaModule != null) {
        return plugin.dependencies + javaModule.asDependency()
      }
    }
    return plugin.dependencies
  }

  private fun IdePlugin.isNonBundled(pluginProvider: PluginProvider): Boolean {
    return pluginId?.let { id ->
      pluginProvider.findPluginById(id) != null
    } ?: false
  }

  private fun IdePlugin.asDependency(): PluginDependency {
    return PluginDependencyImpl(id, false, true)
  }

  private val IdePlugin.id: String
    get() {
      return pluginId ?: pluginName ?: UNKNOWN_PLUGIN_ID
    }
}
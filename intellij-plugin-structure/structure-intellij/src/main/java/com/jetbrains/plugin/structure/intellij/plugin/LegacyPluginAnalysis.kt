package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule

class LegacyPluginAnalysis {
  // https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html#declaring-plugin-dependencies
  fun isLegacyPlugin(plugin: IdePlugin): Boolean = with(plugin) {
    plugin !is IdeModule 
      && !hasPackagePrefix
      && modulesDescriptors.isEmpty()
      && dependencies.all { it.canBeLegacy }
  }

  private val PluginDependency.canBeLegacy: Boolean
    get() = when (this) {
      is PluginV2Dependency -> false
      is ModuleV2Dependency -> false
      else -> !isModule && id != "com.intellij.java"
    }
}
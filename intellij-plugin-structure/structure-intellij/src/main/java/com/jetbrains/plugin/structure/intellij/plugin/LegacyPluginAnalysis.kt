/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule

@Deprecated("See com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier")
class LegacyPluginAnalysis {
  /**
   * Returns `true` if [plugin] is a legacy plugin which is compatible with IntelliJ IDEA only, see
   * https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html#declaring-plugin-dependencies
   */
  @Deprecated("See com.jetbrains.plugin.structure.intellij.verifiers.LegacyIntelliJIdeaPluginVerifier#verify")
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
      is InlineDeclaredModuleV2Dependency -> false
      else -> !isModule && id != "com.intellij.java"
    }
}
/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.analysis

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule

class LegacyPluginAnalysis {
  // https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html#declaring-plugin-dependencies
  fun isLegacyPlugin(plugin: IdePlugin): Boolean = with(plugin) {
    plugin !is IdeModule && !hasPackagePrefix && (hasNoDependencies() || hasNoModuleDependencies())
  }

  private fun IdePlugin.hasNoDependencies() = dependencies.isEmpty()

  private fun IdePlugin.hasNoModuleDependencies() = dependencies.none { it.isModule }
}
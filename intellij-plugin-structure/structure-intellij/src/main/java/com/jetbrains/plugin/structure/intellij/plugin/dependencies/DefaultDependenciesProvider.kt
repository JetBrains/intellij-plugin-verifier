/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider

class DefaultDependenciesProvider(pluginProvider: PluginProvider) : DependenciesProvider {
  private val dependencyTree = DependencyTree(pluginProvider)

  override fun getDependencies(plugin: IdePlugin) = dependencyTree.getTransitiveDependencies(plugin)
}
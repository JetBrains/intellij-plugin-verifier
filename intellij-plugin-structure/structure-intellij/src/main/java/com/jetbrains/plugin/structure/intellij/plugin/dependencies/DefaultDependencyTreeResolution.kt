/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

class DefaultDependencyTreeResolution internal constructor(
  override val dependencyRoot: IdePlugin,
  override val transitiveDependencies: Collection<Dependency>,
  override val missingDependencies: Map<IdePlugin, Set<PluginDependency>>,
  private val graph: DependencyTree.DiGraph<PluginId, Dependency>
) : DependencyTreeResolution {

  override fun forEach(action: (PluginId, PluginDependency) -> Unit) {
    graph.forEachAdjacency { pluginId, dependencies ->
      dependencies.forEach { dep ->
        dep.pluginDependency?.let { action(pluginId, it) }
      }
    }
  }
}
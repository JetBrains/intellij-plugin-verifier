/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

interface DependencyTreeResolution {
  val dependencyRoot: IdePlugin
  val missingDependencies: Map<IdePlugin, Set<PluginDependency>>
  val transitiveDependencies: Collection<Dependency>
  fun forEach(action: (PluginId, PluginDependency) -> Unit)
}
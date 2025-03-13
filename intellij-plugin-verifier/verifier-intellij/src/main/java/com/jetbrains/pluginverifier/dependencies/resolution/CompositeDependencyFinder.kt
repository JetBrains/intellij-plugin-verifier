/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

/**
 * [DependencyFinder] that subsequently delegates the search
 * to the [dependencyFinders] until the dependency is resolved.
 *
 * If the dependency is not resolved by any of the [dependencyFinders],
 * the [DependencyFinder.Result.NotFound] is returned.
 */
class CompositeDependencyFinder(private val dependencyFinders: List<DependencyFinder>) : DependencyFinder {
  override val presentableName
    get() = dependencyFinders.joinToString { it.presentableName }

  override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result {
    return dependencyFinders.asSequence()
      .map { it.findPluginDependency(dependencyId, isModule) }
      .firstOrNull { it !is DependencyFinder.Result.NotFound }
      ?: DependencyFinder.Result.NotFound("Dependency '$dependencyId'${if (isModule) " (module)" else ""} is not resolved. It was searched in the following locations: $presentableName")
  }

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    return dependencyFinders.asSequence()
      .map { it.findPluginDependency(dependency) }
      .firstOrNull { it !is DependencyFinder.Result.NotFound }
      ?: DependencyFinder.Result.NotFound("Dependency '$dependency' is not resolved. It was searched in the following locations: $presentableName")
  }
}
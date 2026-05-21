/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * [DependencyFinder] that subsequently delegates the search
 * to the [dependencyFinders] until the dependency is resolved.
 *
 * If the dependency is not resolved by any of the [dependencyFinders],
 * the [DependencyFinder.Result.NotFound] is returned.
 */
class CompositeDependencyFinder(private val dependencyFinders: List<DependencyFinder>) : DependencyFinder {
  private companion object {
    private val LOG: Logger = LoggerFactory.getLogger(CompositeDependencyFinder::class.java)
  }

  override val presentableName
    get() = dependencyFinders.joinToString { it.presentableName }

  override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result {
    val kind = if (isModule) "module" else "plugin"
    val label = "'$dependencyId'"
    return resolve(kind, label) { it.findPluginDependency(dependencyId, isModule) }
      ?: notFound("Dependency '$dependencyId'${if (isModule) " (module)" else ""} is not resolved. It was searched in the following locations: $presentableName", kind, label)
  }

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    val kind = if (dependency.isModule) "module" else "plugin"
    val label = "'$dependency'"
    return resolve(kind, label) { it.findPluginDependency(dependency) }
      ?: notFound("Dependency '$dependency' is not resolved. It was searched in the following locations: $presentableName", kind, label)
  }

  private inline fun resolve(
    kind: String,
    label: String,
    findIn: (DependencyFinder) -> DependencyFinder.Result
  ): DependencyFinder.Result? {
    for (finder in dependencyFinders) {
      val result = findIn(finder)
      if (result !is DependencyFinder.Result.NotFound) {
        LOG.debug("Dependency {} {} resolved by {}", kind, label, finder.presentableName)
        return result
      } else {
        LOG.debug("Dependency {} {} not found in {}: {}", kind, label, finder.presentableName, result.reason)
      }
    }
    return null
  }

  private fun notFound(reason: String, kind: String, label: String): DependencyFinder.Result.NotFound {
    LOG.info("Dependency {} {} could not be resolved by any finder ({})", kind, label, presentableName)
    return DependencyFinder.Result.NotFound(reason)
  }
}
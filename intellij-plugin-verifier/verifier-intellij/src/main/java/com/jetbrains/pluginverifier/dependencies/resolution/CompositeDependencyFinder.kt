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

  override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
    var lastNotFound: DependencyFinder.Result.NotFound? = null
    for (dependencyFinder in dependencyFinders) {
      val findResult = dependencyFinder.findPluginDependency(dependency)
      if (findResult !is DependencyFinder.Result.NotFound) {
        return findResult
      }
      lastNotFound = findResult
    }
    return lastNotFound ?: DependencyFinder.Result.NotFound("Dependency $dependency is not resolved")
  }

}
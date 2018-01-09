package com.jetbrains.pluginverifier.dependencies.resolution

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency

/**
 * [DependencyFinder] that subsequently delegates the search
 * to the [dependencyFinders] until the dependency is resolved.
 *
 * If the dependency is not resolved by any of the [dependencyFinders],
 * the [DependencyFinder.Result.NotFound] is returned.
 */
class ChainDependencyFinder(private val dependencyFinders: List<DependencyFinder>) : DependencyFinder {

  override fun findPluginDependency(dependency: PluginDependency) =
      dependencyFinders
          .asSequence()
          .map { it.findPluginDependency(dependency) }
          .firstOrNull { it !is DependencyFinder.Result.NotFound }
          ?: DependencyFinder.Result.NotFound("Dependency $dependency is not resolved")

}
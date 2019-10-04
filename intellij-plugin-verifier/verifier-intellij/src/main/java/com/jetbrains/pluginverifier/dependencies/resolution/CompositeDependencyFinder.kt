package com.jetbrains.pluginverifier.dependencies.resolution

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
    var lastNotFound: DependencyFinder.Result.NotFound? = null
    for (dependencyFinder in dependencyFinders) {
      val findResult = dependencyFinder.findPluginDependency(dependencyId, isModule)
      if (findResult !is DependencyFinder.Result.NotFound) {
        return findResult
      }
      lastNotFound = findResult
    }
    return lastNotFound ?: DependencyFinder.Result.NotFound("Dependency '$dependencyId'${if (isModule) " (module)" else ""} is not resolved. It was searched in the following locations: $presentableName")
  }

}
package com.jetbrains.pluginverifier.dependencies

/**
 * Contains a path of [DependencyNode]s in the [DependenciesGraph]
 * starting at the [DependenciesGraph.verifiedPlugin] and ending in some
 * [missing] [missingDependency] dependency.
 */
data class MissingDependencyPath(val path: List<DependencyNode>,
                                 val missingDependency: MissingDependency) {
  override fun toString() = path.joinToString(" ---X--> ") + " ---X--> " + missingDependency
}
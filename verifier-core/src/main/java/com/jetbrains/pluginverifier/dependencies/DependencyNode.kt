package com.jetbrains.pluginverifier.dependencies

/**
 * Represents a node in the [DependenciesGraph].
 *
 * The node is a plugin [pluginId] and [version].
 *
 * The plugin could depend on modules and plugins that might not
 * be resolved. Those modules and plugins are  [missingDependencies].
 */
data class DependencyNode(
    val pluginId: String,
    val version: String,
    val missingDependencies: List<MissingDependency>
) {
  override fun toString() = "$pluginId:$version"
}
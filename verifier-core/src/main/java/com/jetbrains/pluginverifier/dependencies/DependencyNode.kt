package com.jetbrains.pluginverifier.dependencies

import java.io.Serializable

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
) : Serializable {
  override fun toString() = "$pluginId:$version"

  companion object {
    private const val serialVersionUID = 0L
  }
}
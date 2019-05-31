package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder

/**
 * Node in the internal dependencies graph keeping the resolved plugin's resources.
 */
data class DepVertex(val pluginId: String, val dependencyResult: DependencyFinder.Result) {

  override fun equals(other: Any?) = other is DepVertex && pluginId == other.pluginId

  override fun hashCode() = pluginId.hashCode()
}
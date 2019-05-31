package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder

/**
 * Node in the internal dependencies graph keeping the resolved plugin's resources.
 */
data class DepVertex(val dependencyId: String, val dependencyResult: DependencyFinder.Result) {

  override fun equals(other: Any?) = other is DepVertex && dependencyId == other.dependencyId

  override fun hashCode() = dependencyId.hashCode()
}
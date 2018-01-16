package com.jetbrains.pluginverifier.dependencies.graph

import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder

/**
 * Node in the internal dependencies graph
 * keeping the resolved plugin's resources.
 *
 * It contains the [resolution result] [dependencyResult]
 * of which this vertex originates.
 *
 * In the final [results] [com.jetbrains.pluginverifier.results.VerificationResult] the [DepVertex] will be converted to the
 * API version [dependency node] [com.jetbrains.pluginverifier.dependencies.DependencyNode].
 */
data class DepVertex(val dependencyId: String,
                     val dependencyResult: DependencyFinder.Result) {

  override fun equals(other: Any?) = other is DepVertex && dependencyId == other.dependencyId

  override fun hashCode() = dependencyId.hashCode()
}
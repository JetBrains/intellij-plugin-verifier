/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.dependencies.processing.DependenciesGraphCycleFinder

/**
 * Graph of [plugin dependencies] [com.jetbrains.plugin.structure.intellij.plugin.PluginDependency]
 * built for the plugin verification.
 *
 * The graph is stored as a set of [vertices] and [edges]. The starting vertex is [verifiedPlugin].
 */
data class DependenciesGraph(
  val verifiedPlugin: DependencyNode,
  val vertices: Set<DependencyNode>,
  val edges: Set<DependencyEdge>,
  val missingDependencies: Map<DependencyNode, Set<MissingDependency>>
) {

  /**
   * Returns all missing dependencies required by the verified plugin directly.
   */
  fun getDirectMissingDependencies(): Set<MissingDependency> =
    missingDependencies.getOrDefault(verifiedPlugin, emptySet())

  /**
   * Returns all edges starting at the specified node.
   */
  fun getEdgesFrom(dependencyNode: DependencyNode): List<DependencyEdge> =
    edges.filter { it.from == dependencyNode }

  /**
   * Returns all cycles in this graph.
   * The dependencies cycles are harmful and should be fixed.
   */
  fun getAllCycles(): List<List<DependencyNode>> =
    DependenciesGraphCycleFinder(this)
      .findAllCycles()
      .filter { verifiedPlugin in it }

  override fun toString() = DependenciesGraphPrettyPrinter(this).prettyPresentation()

}

/**
 * Represents an edge in the [DependenciesGraph],
 * which is a [dependency] of the plugin [from] to the plugin [to].
 */
data class DependencyEdge(
  val from: DependencyNode,
  val to: DependencyNode,
  val dependency: PluginDependency
) {
  override fun toString() = if (dependency.isOptional) "$from ---optional---> $to" else "$from ---> $to"
}

/**
 * Represents a node in the [DependenciesGraph].
 *
 * The node is a plugin [pluginId] and [version].
 */
data class DependencyNode(val pluginId: String, val version: String, val plugin: IdePlugin? = null) {
  override fun toString() = "$pluginId:$version"
}

/**
 * Represents a [dependency] of the [verified plugin] [DependenciesGraph.verifiedPlugin]
 * that was not resolved due to [missingReason].
 */
data class MissingDependency(val dependency: PluginDependency, val missingReason: String) {
  override fun toString() = "$dependency: $missingReason"
}
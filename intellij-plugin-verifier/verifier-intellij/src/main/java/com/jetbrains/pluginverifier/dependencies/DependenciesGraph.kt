/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginAware
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
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
   * Checks for cycles in this graph that involve the verified plugin. If one is found, [fn] is invoked with it.
   * The dependencies cycles are harmful and should be fixed.
   */
  fun checkForCycle(fn: (List<DependencyNode>) -> Unit) =
    DependenciesGraphCycleFinder(this)
      .checkForCycle(fn)

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
 */
sealed class DependencyNode {
  abstract val id: String
  abstract val version: String

  /**
   * Represents a dependency of the underlying [IdePlugin], optionally storing aliases as metadata.
   * Only the IdePlugin takes part in equality/hashCode checks!
   */
  class PluginDependency(override val plugin: IdePlugin): DependencyNode(), PluginAware {
    override val id = plugin.id
    override val version = plugin.pluginVersion ?: UNKNOWN_VERSION

    private val _aliases: MutableSet<String> = mutableSetOf()
    val aliases: Set<String> get() = _aliases

    fun addAlias(alias: String) {
      _aliases += alias
    }

    override fun toString() = "$id:$version" + if (aliases.isNotEmpty()) " (aliased ${aliases.joinToString(" ")})" else ""

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as PluginDependency

      return plugin == other.plugin
    }

    override fun hashCode(): Int {
      return plugin.hashCode()
    }
  }

  data class IdAndVersionDependency(override val id: String, override val version: String) : DependencyNode() {
    override fun toString() = "$id:$version"
  }

  companion object {
    fun dependencyNode(id: String, version: String) = IdAndVersionDependency(id, version)
    fun dependencyNode(plugin: IdePlugin) = PluginDependency(plugin)
    fun dependencyNode(alias: String, plugin: IdePlugin) = PluginDependency(plugin).also { it.addAlias(alias) }

    fun mergeAliasesIntoFirst(node1: DependencyNode.PluginDependency, node2: DependencyNode.PluginDependency): DependencyNode.PluginDependency {
      node2.aliases.forEach(node1::addAlias)
      return node1
    }
  }
}

/**
 * Represents a [dependency] of the [verified plugin] [DependenciesGraph.verifiedPlugin]
 * that was not resolved due to [missingReason].
 */
data class MissingDependency(val dependency: PluginDependency, val missingReason: String) {
  override fun toString() = "$dependency: $missingReason"
}
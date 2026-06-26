/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule

/**
 * String-only representation of a plugin dependency identifier, used after dependency resolution.
 * Mirrors [com.jetbrains.plugin.structure.intellij.plugin.PluginDependency] but holds no live objects.
 */
data class ResolvedPluginDependency(val id: String, val isOptional: Boolean, val isModule: Boolean) {
  override fun toString() = if (isOptional) "$id (optional)" else id
}

/**
 * A node in [ResolvedDependenciesGraph]. Holds only string identifiers and carries no references
 * to [com.jetbrains.plugin.structure.intellij.plugin.IdePlugin], allowing resolved plugin objects
 * to be garbage-collected after verification completes.
 */
data class ResolvedDependencyNode(
  val id: String,
  val version: String,
  val aliases: Set<String> = emptySet(),
  val isProductModule: Boolean = false
) {
  override fun toString() = "$id:$version" + if (aliases.isNotEmpty()) " (aliased ${aliases.joinToString(" ")})" else ""
}

/**
 * An edge in [ResolvedDependenciesGraph].
 */
data class ResolvedDependencyEdge(
  val from: ResolvedDependencyNode,
  val to: ResolvedDependencyNode,
  val dependency: ResolvedPluginDependency
) {
  override fun toString() = if (dependency.isOptional) "$from ---optional---> $to" else "$from ---> $to"
}

/**
 * A dependency of the verified plugin that could not be resolved during verification.
 */
data class ResolvedMissingDependency(val dependency: ResolvedPluginDependency, val missingReason: String) {
  override fun toString() = "$dependency: $missingReason"
}

/**
 * Post-resolution, string-only dependency graph stored in [com.jetbrains.pluginverifier.PluginVerificationResult.Verified].
 *
 * Built by [DependenciesGraph.toResolved] after dependency resolution completes. Retains no references to
 * [com.jetbrains.plugin.structure.intellij.plugin.IdePlugin] instances, so they can be garbage-collected
 * once verification finishes.
 */
data class ResolvedDependenciesGraph(
  val verifiedPlugin: ResolvedDependencyNode,
  val vertices: Set<ResolvedDependencyNode>,
  val edges: Set<ResolvedDependencyEdge>,
  val missingDependencies: Map<ResolvedDependencyNode, Set<ResolvedMissingDependency>>
) {
  fun getDirectMissingDependencies(): Set<ResolvedMissingDependency> =
    missingDependencies.getOrDefault(verifiedPlugin, emptySet())

  fun getEdgesFrom(node: ResolvedDependencyNode): List<ResolvedDependencyEdge> =
    edges.filter { it.from == node }
}

/**
 * Converts the fat [DependenciesGraph] (which holds live [com.jetbrains.plugin.structure.intellij.plugin.IdePlugin]
 * references) into a [ResolvedDependenciesGraph] containing only string identifiers.
 *
 * Call this before storing the graph in a [com.jetbrains.pluginverifier.PluginVerificationResult] so that
 * the plugin objects can be garbage-collected.
 */
fun DependenciesGraph.toResolved(): ResolvedDependenciesGraph {
  val allNodes: Set<DependencyNode> = mutableSetOf<DependencyNode>().apply {
    add(verifiedPlugin)
    addAll(vertices)
    edges.forEach { add(it.from); add(it.to) }
    addAll(missingDependencies.keys)
  }
  val nodeMap = allNodes.associate { node ->
    val isProductModule = node is DependencyNode.PluginDependency && node.plugin is IdeModule
    val aliases = if (node is DependencyNode.PluginDependency) node.aliases.toSet() else emptySet()
    node to ResolvedDependencyNode(node.id, node.version, aliases, isProductModule)
  }

  val resolvedEdges = edges.mapTo(hashSetOf()) { edge ->
    ResolvedDependencyEdge(
      nodeMap.getValue(edge.from),
      nodeMap.getValue(edge.to),
      ResolvedPluginDependency(edge.dependency.id, edge.dependency.isOptional, edge.dependency.isModule)
    )
  }

  val resolvedMissingDeps = missingDependencies.entries.associate { (node, missing) ->
    nodeMap.getValue(node) to missing.mapTo(hashSetOf()) { md ->
      ResolvedMissingDependency(
        ResolvedPluginDependency(md.dependency.id, md.dependency.isOptional, md.dependency.isModule),
        md.missingReason
      )
    }
  }

  return ResolvedDependenciesGraph(
    nodeMap.getValue(verifiedPlugin),
    nodeMap.values.toSet(),
    resolvedEdges,
    resolvedMissingDeps
  )
}

/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import com.jetbrains.pluginverifier.PluginVerifierBatchContext
import java.util.function.Function

/**
 * String-only representation of a plugin dependency identifier, used after dependency resolution.
 * Mirrors [com.jetbrains.plugin.structure.intellij.plugin.PluginDependency] but holds no live objects.
 */
data class ResolvedPluginDependency(val id: String,
                                    val isOptional: Boolean,
                                    val isModule: Boolean,
                                    val isContentModule: Boolean = false) {
  override fun toString() = if (isOptional) "$id (optional)" else id
}

/**
 * A node in [ResolvedDependenciesGraph]. Holds only string identifiers and carries no references
 * to [com.jetbrains.plugin.structure.intellij.plugin.IdePlugin], allowing resolved plugin objects
 * to be garbage-collected after verification completes.
 *
 * Only [id], [version], [moduleOwnerId], [isProductModule] and [isContentModuleDeclaration]
 * take part in [equals] and [hashCode], [aliases] are metadata only.
 * Platform nodes carry hundreds of aliases and nodes are hashed in every graph-building and reporting step,
 * so the hash code is computed once and cached.
 */
data class ResolvedDependencyNode(
  val id: String,
  val version: String,
  val aliases: Set<String> = emptySet(),
  val isProductModule: Boolean = false,
  val moduleOwnerId: String? = null,
  val isContentModuleDeclaration: Boolean = false
) {

  private val hash: Int = run {
    var result = id.hashCode()
    result = 31 * result + version.hashCode()
    result = 31 * result + isProductModule.hashCode()
    result = 31 * result + moduleOwnerId.hashCode()
    result = 31 * result + isContentModuleDeclaration.hashCode()
    result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ResolvedDependencyNode) return false
    return hash == other.hash
      && id == other.id
      && version == other.version
      && isProductModule == other.isProductModule
      && moduleOwnerId == other.moduleOwnerId
      && isContentModuleDeclaration == other.isContentModuleDeclaration
  }

  override fun hashCode(): Int = hash

  // Deliberately omits [aliases]: platform nodes carry hundreds of module aliases and this string
  // is emitted once per referencing edge in dependency reports, multiplying report size by orders of magnitude.
  // Use [toStringWithAliases] where the full presentation is wanted (e.g. a node's first occurrence in a report).
  override fun toString(): String {
    return if (isContentModuleDeclaration) {
      "$moduleOwnerId:$version/$id"
    } else {
      if (moduleOwnerId != null) {
        "$moduleOwnerId:$version/$id"
      } else {
        "$id:$version"
      }
    }
  }

  fun toStringWithAliases() = toString() + if (aliases.isNotEmpty()) " (aliased ${aliases.joinToString(" ")})" else ""
}

/**
 * An edge in [ResolvedDependenciesGraph].
 *
 * The hash code is computed once and cached, as edges are hashed repeatedly while building and reporting graphs.
 */
data class ResolvedDependencyEdge(
  val from: ResolvedDependencyNode,
  val to: ResolvedDependencyNode,
  val dependency: ResolvedPluginDependency
) {
  private val hash: Int = 31 * (31 * from.hashCode() + to.hashCode()) + dependency.hashCode()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ResolvedDependencyEdge) return false
    return hash == other.hash
      && from == other.from
      && to == other.to
      && dependency == other.dependency
  }

  override fun hashCode(): Int = hash

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

  @Deprecated("Build an index from 'edges' when repeated lookups are needed")
  fun getEdgesFrom(node: ResolvedDependencyNode): List<ResolvedDependencyEdge> =
    edges.filter { it.from == node }

  /**
   * Returns a slim copy of this graph that keeps only what is needed once the full graph has been reported:
   * - the [verifiedPlugin];
   * - the direct edges of the [verifiedPlugin];
   * - the edges whose dependency matches a direct (resolved or missing) dependency of the [verifiedPlugin],
   *   so that looking up how a direct dependency was resolved keeps working;
   * - the direct missing dependencies, see [getDirectMissingDependencies].
   *
   * The transitive dependencies graph of the verified plugin spans the whole IDE module graph
   * and is the largest part of a retained verification result.
   */
  fun retainDirectDependencies(): ResolvedDependenciesGraph {
    val directMissingDependencies = missingDependencies[verifiedPlugin]
    val directEdges = edges.filter { it.from == verifiedPlugin }
    val directDependencies = HashSet<ResolvedPluginDependency>().apply {
      directEdges.mapTo(this) { it.dependency }
      directMissingDependencies?.mapTo(this) { it.dependency }
    }
    // Direct edges come first, so a lookup of a direct dependency prefers the edge of the verified plugin.
    val retainedEdges = LinkedHashSet<ResolvedDependencyEdge>(directEdges).apply {
      edges.filterTo(this) { it.dependency in directDependencies }
    }
    val retainedVertices = LinkedHashSet<ResolvedDependencyNode>().apply {
      add(verifiedPlugin)
      retainedEdges.forEach { add(it.from); add(it.to) }
    }
    return ResolvedDependenciesGraph(
      verifiedPlugin,
      retainedVertices,
      retainedEdges,
      directMissingDependencies?.let { mapOf(verifiedPlugin to it) } ?: emptyMap()
    )
  }
}

/**
 * Converts the fat [DependenciesGraph] (which holds live [com.jetbrains.plugin.structure.intellij.plugin.IdePlugin]
 * references) into a [ResolvedDependenciesGraph] containing only string identifiers.
 *
 * Call this before storing the graph in a [com.jetbrains.pluginverifier.PluginVerificationResult] so that
 * the plugin objects can be garbage-collected.
 */
fun DependenciesGraph.toResolved(batchContext: PluginVerifierBatchContext? = null): ResolvedDependenciesGraph {
  val cache = batchContext?.deduplicationMap ?: HashMap()

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> T.dedup(): T = cache.computeIfAbsent(this, Function.identity()) as T

  val allNodes: Set<DependencyNode> = mutableSetOf<DependencyNode>().apply {
    add(verifiedPlugin)
    addAll(vertices)
    edges.forEach { add(it.from); add(it.to) }
    addAll(missingDependencies.keys)
  }

  val nodeMap = allNodes.associateWith { node ->
    val isProductModule = node is DependencyNode.PluginDependency && node.plugin is IdeModule
    val aliases = (node as? DependencyNode.PluginDependency)?.aliases
      ?.takeIf { it.isNotEmpty() }
      ?.mapTo(HashSet()) { it.dedup() }
      ?: emptySet()
    val moduleOwnerId = when (node) {
      is DependencyNode.ModuleDependency -> node.plugin.id.dedup()
      is DependencyNode.ContentModuleDeclaration -> node.owner.id.dedup()
      else -> null
    }
    val resolvedNode = ResolvedDependencyNode(
      node.id.dedup(), node.version.dedup(), aliases, isProductModule, moduleOwnerId,
      isContentModuleDeclaration = node is DependencyNode.ContentModuleDeclaration
    )
    // Aliases do not take part in node equality. Reuse the shared node only if it carries the same aliases,
    // so that this graph keeps presenting its own aliases.
    resolvedNode.dedup().takeIf { it.aliases == aliases } ?: resolvedNode
  }

  // The edge set itself is not deduplicated: edge sets of distinct plugins almost never match as a whole,
  // and keeping them in the batch-wide deduplication map would retain them until the batch finishes.
  val resolvedEdges = java.util.Set.copyOf(edges.map { edge ->
    val isContentModule = edge.to is DependencyNode.ModuleDependency
    val dependency = ResolvedPluginDependency(
      edge.dependency.id.dedup(),
      edge.dependency.isOptional,
      edge.dependency.isModule, isContentModule).dedup()
    val resolvedEdge = ResolvedDependencyEdge(
      nodeMap.getValue(edge.from),
      nodeMap.getValue(edge.to),
      dependency
    )
    // Same as for nodes: the shared edge must point to nodes carrying the aliases of this graph.
    resolvedEdge.dedup().takeIf { it.from.aliases == resolvedEdge.from.aliases && it.to.aliases == resolvedEdge.to.aliases } ?: resolvedEdge
  })

  val resolvedMissingDeps = missingDependencies.entries.associate { (node, missing) ->
    nodeMap.getValue(node) to missing.mapTo(hashSetOf()) { md ->
      ResolvedMissingDependency(
        ResolvedPluginDependency(md.dependency.id.dedup(), md.dependency.isOptional, md.dependency.isModule, false).dedup(),
        md.missingReason
      ).dedup()
    }.dedup()
  }

  return ResolvedDependenciesGraph(
    nodeMap.getValue(verifiedPlugin),
    nodeMap.values.toSet(),
    resolvedEdges,
    resolvedMissingDeps
  )
}

/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTreeResolution
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginAware
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.pluginDependency
import com.jetbrains.pluginverifier.dependencies.DependencyNode.Companion.dependencyNode
import java.util.concurrent.ConcurrentHashMap

internal const val UNKNOWN_VERSION = "unknown version"

private const val DEFAULT_MISSING_DEPENDENCY_REASON = "Unavailable"

/**
 * An adapter between the dependency tree provided by _IntelliJ Structure Library_ and Plugin Verifier dependency tree.
 */
class DependenciesGraphProvider {
  private val normalizer = DependencyNodeNormalizer()

  fun getDependenciesGraph(dependencyTreeResolution: DependencyTreeResolution): DependenciesGraph {
    val verifiedPlugin = newDependencyNode(dependencyTreeResolution.dependencyRoot)
    val transitiveDependencyVertices = dependencyTreeResolution.getTransitiveDependencyVertices()
    val vertices = transitiveDependencyVertices + verifiedPlugin
    val edges = dependencyTreeResolution.getEdges()
    val missingDependencies = dependencyTreeResolution.getMissingDependencies()

    return DependenciesGraph(verifiedPlugin, vertices, edges, missingDependencies)
  }

  private fun DependencyTreeResolution.getTransitiveDependencyVertices(): Set<DependencyNode> {
    return transitiveDependencies.flatMapTo(LinkedHashSet()) {
      when (it) {
        is Dependency.Module -> it.getVertices()
        is Dependency.Plugin -> setOf(newDependencyNode(it.plugin))
        Dependency.None -> emptySet()
      }
    }
  }

  private fun DependencyTreeResolution.getEdges(): Set<DependencyEdge> {
    val edges = LinkedHashSet<DependencyEdge>()
    forEach { from, dependency ->
      dependency.pluginDependency?.let { pluginDependency ->
        require(from is PluginAware && dependency is PluginAware) // Invariant by the pluginDependency getter returning non-null

        edges += DependencyEdge(
          newDependencyNode(from.plugin),
          newDependencyNode(dependency.plugin),
          pluginDependency
        )
      }
    }
    return edges
  }

  private class DependencyNodeNormalizer {
    private val cache = ConcurrentHashMap<DependencyNode.PluginDependency, DependencyNode.PluginDependency>()

    fun intern(dependencyNode: DependencyNode): DependencyNode = when (dependencyNode) {
      is DependencyNode.PluginDependency -> cache.merge(dependencyNode, dependencyNode, DependencyNode::mergeAliasesIntoFirst)!! // merge function never returns null
      is DependencyNode.IdAndVersionDependency -> dependencyNode
    }
  }

  private fun DependencyTreeResolution.getMissingDependencies(): Map<DependencyNode, Set<MissingDependency>> {
    return missingDependencies.map { (plugin, dependencies) ->
      val pluginNode = newDependencyNode(plugin)
      val dependencyNodes = dependencies.mapTo(mutableSetOf()) {
        MissingDependency(it, DEFAULT_MISSING_DEPENDENCY_REASON)
      }
      pluginNode to dependencyNodes
    }.toMap()
  }

  private fun Dependency.Module.getVertices(): List<DependencyNode> {
    val vertices = mutableListOf<DependencyNode>()
    vertices += newDependencyNode(plugin)
    if (id != plugin.id) {
      vertices += newDependencyNode(id, plugin)
    }

    val definedModuleNodes = plugin.definedModules.map { alias -> newDependencyNode(alias, plugin) }
    vertices += definedModuleNodes

    return vertices
  }

  private fun newDependencyNode(plugin: IdePlugin) = normalizer.intern(DependencyNode.PluginDependency(plugin))

  private fun newDependencyNode(alias: String, plugin: IdePlugin) = normalizer.intern(dependencyNode(alias, plugin))
}

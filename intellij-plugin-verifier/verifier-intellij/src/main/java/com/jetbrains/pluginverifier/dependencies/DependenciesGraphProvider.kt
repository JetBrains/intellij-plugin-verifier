/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTreeResolution
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginAware
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.pluginDependency
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

private const val UNKNOWN_VERSION = "unknown version"

private const val DEFAULT_MISSING_DEPENDENCY_REASON = "Unavailable"

/**
 * An adapter between the dependency tree provided by _IntelliJ Structure Library_ and Plugin Verifier dependency tree.
 */
class DependenciesGraphProvider {
  fun getDependenciesGraph(dependencyTreeResolution: DependencyTreeResolution): DependenciesGraph {
    val verifiedPlugin = DependencyNode(dependencyTreeResolution.dependencyRoot.id, version = UNKNOWN_VERSION, dependencyTreeResolution.dependencyRoot)
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
        is Dependency.Plugin -> setOf(DependencyNode(it.id, version = UNKNOWN_VERSION, it.plugin))
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
          DependencyNode(from.id, UNKNOWN_VERSION, from.plugin).intern(),
          DependencyNode(dependency.id, UNKNOWN_VERSION, dependency.plugin).intern(),
          pluginDependency
        )
      }
    }
    return edges
  }

  private val pluginDependencyCache = ConcurrentHashMap<PluginDependency, PluginDependency>()
  private fun PluginDependency.intern(): PluginDependency = pluginDependencyCache.computeIfAbsent(this, Function.identity())
  private val dependencyNodeCache = ConcurrentHashMap<DependencyNode, DependencyNode>()
  private fun DependencyNode.intern(): DependencyNode = dependencyNodeCache.computeIfAbsent(this, Function.identity())

  private fun DependencyTreeResolution.getMissingDependencies(): Map<DependencyNode, Set<MissingDependency>> {
    return missingDependencies.map { (plugin, dependencies) ->
      val pluginNode = DependencyNode(plugin.id, version = UNKNOWN_VERSION, plugin)
      val dependencyNodes = dependencies.mapTo(mutableSetOf()) {
        MissingDependency(it, DEFAULT_MISSING_DEPENDENCY_REASON)
      }
      pluginNode to dependencyNodes
    }.toMap()
  }

  private fun Dependency.Module.getVertices(): List<DependencyNode> {
    val vertices = mutableListOf<DependencyNode>()
    vertices += DependencyNode(id, version = UNKNOWN_VERSION, plugin)
    vertices += DependencyNode(plugin.id, version = UNKNOWN_VERSION, plugin)

    val definedModuleNodes = plugin.definedModules.map { alias -> DependencyNode(alias, version = UNKNOWN_VERSION, plugin) }
    vertices += definedModuleNodes

    return vertices
  }
}
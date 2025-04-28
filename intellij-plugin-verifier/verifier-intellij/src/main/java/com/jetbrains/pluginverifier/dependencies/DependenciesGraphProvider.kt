/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTreeResolution
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id

private const val UNKNOWN_VERSION = "unknown version"

private const val DEFAULT_MISSING_DEPENDENCY_REASON = "Unavailable"

class DependenciesGraphProvider {
  fun getDependenciesGraph(dependencyTreeResolution: DependencyTreeResolution): DependenciesGraph {
    val verifiedPlugin = DependencyNode(dependencyTreeResolution.dependencyRoot.id, version = UNKNOWN_VERSION)
    val transitiveDependencyVertices = dependencyTreeResolution.getTransitiveDependencyVertices()
    val vertices = transitiveDependencyVertices + verifiedPlugin
    val edges = dependencyTreeResolution.getEdges()
    val missingDependencies = dependencyTreeResolution.getMissingDependencies()

    return DependenciesGraph(verifiedPlugin, vertices, edges, missingDependencies)
  }

  private fun DependencyTreeResolution.getTransitiveDependencyVertices(): List<DependencyNode> {
    return transitiveDependencies.flatMap {
      when (it) {
        is Dependency.Module -> it.getVertices()
        is Dependency.Plugin -> setOf(DependencyNode(it.id, version = UNKNOWN_VERSION))
        Dependency.None -> emptySet()
      }
    }
  }

  private fun DependencyTreeResolution.getEdges(): List<DependencyEdge> {
    val edges = mutableListOf<DependencyEdge>()
    forEach { id, dependency ->
      edges += DependencyEdge(DependencyNode(id,
        "unknown version"), DependencyNode(dependency.id, UNKNOWN_VERSION), dependency)
    }
    return edges
  }

  private fun DependencyTreeResolution.getMissingDependencies(): Map<DependencyNode, Set<MissingDependency>> {
    return missingDependencies.map { (plugin, dependencies) ->
      val pluginNode = DependencyNode(plugin.id, version = UNKNOWN_VERSION)
      val dependencyNodes = dependencies.mapTo(mutableSetOf()) {
        MissingDependency(it, DEFAULT_MISSING_DEPENDENCY_REASON)
      }
      pluginNode to dependencyNodes
    }.toMap()
  }

  private fun Dependency.Module.getVertices(): List<DependencyNode> {
    val vertices = mutableListOf<DependencyNode>()
    vertices += DependencyNode(id, version = UNKNOWN_VERSION)
    vertices += DependencyNode(plugin.id, version = UNKNOWN_VERSION)

    val definedModuleNodes = plugin.definedModules.map { alias -> DependencyNode(alias, version = UNKNOWN_VERSION)
      DependencyNode(id, version = UNKNOWN_VERSION)
    }
    vertices += definedModuleNodes

    return vertices
  }
}
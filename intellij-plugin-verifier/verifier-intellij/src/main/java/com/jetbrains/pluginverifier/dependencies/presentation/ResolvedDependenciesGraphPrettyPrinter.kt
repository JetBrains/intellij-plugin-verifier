/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.presentation

import com.jetbrains.pluginverifier.dependencies.ResolvedDependenciesGraph
import com.jetbrains.pluginverifier.dependencies.ResolvedDependencyEdge
import com.jetbrains.pluginverifier.dependencies.ResolvedDependencyNode

/**
 * Provides the [prettyPresentation] method that prints a [ResolvedDependenciesGraph] in
 * a fancy way like the 'gradle dependencies' does:
 *
 * ```
 * start:1.0
 * +--- b:1.0
 * |    +--- c:1.0
 * |    |    +--- (failed) e: plugin e is not found
 * |    |    +--- (failed) f (optional): plugin e is not found
 * |    |    \--- (optional) optional.module:IU-181.1 [declaring module optional.module]
 * |    \--- some.module:IU-181.1 [product module]
 * \--- c:1.0 (*)
 * ```
 */
class ResolvedDependenciesGraphPrettyPrinter(private val graph: ResolvedDependenciesGraph) {

  private val visitedNodes = hashSetOf<ResolvedDependencyNode>()

  fun prettyPresentation(): String {
    val result = StringBuilder()
    visitedNodes.add(graph.verifiedPlugin)
    // First occurrence carries the aliases; repeated occurrences are printed as plain "id:version (*)".
    result.append(graph.verifiedPlugin.toStringWithAliases())
    appendChildren(graph.verifiedPlugin, result, "")
    return result.toString()
  }

  private fun appendChildren(currentNode: ResolvedDependencyNode, result: StringBuilder, childrenPrefix: String) {
    val missingDependencies = graph.missingDependencies
      .getOrDefault(currentNode, emptySet())
      .sortedBy { it.dependency.id }

    val directEdges = graph.getEdgesFrom(currentNode)
      .sortedWith(
        compareBy<ResolvedDependencyEdge> { if (it.dependency.isOptional) 1 else -1 }
          .thenBy { if (it.dependency.isModule) 1 else -1 }
          .thenBy { it.dependency.id }
          .thenBy { it.to.id }
          .thenBy { it.to.version }
      )

    val childrenCount = missingDependencies.size + directEdges.size
    var childIndex = 0

    for (missingDependency in missingDependencies) {
      val isLastChild = ++childIndex == childrenCount
      result.append('\n').append(childrenPrefix).append(if (isLastChild) "\\--- " else "+--- ")
      result.append("(failed) ${missingDependency.dependency}: ${missingDependency.missingReason}")
    }

    for (edge in directEdges) {
      val isLastChild = ++childIndex == childrenCount
      result.append('\n').append(childrenPrefix).append(if (isLastChild) "\\--- " else "+--- ")
      if (edge.dependency.isOptional) {
        result.append("(optional) ")
      }
      val to = edge.to
      val alreadyVisited = to in visitedNodes
      if (alreadyVisited) {
        result.append("$to (*)")
      } else {
        visitedNodes.add(to)
        result.append(to.toStringWithAliases())
      }
      if (to.isProductModule) {
        result.append(" [product module]")
      } else if (edge.dependency.isModule) {
        result.append(" [declaring module ${edge.dependency.id}]")
      }
      if (!alreadyVisited) {
        appendChildren(to, result, childrenPrefix + if (isLastChild) "     " else "|    ")
      }
    }
  }
}

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

  fun prettyPresentation(): String =
    recursivelyCalculateLines(graph.verifiedPlugin).joinToString(separator = "\n")

  private fun recursivelyCalculateLines(currentNode: ResolvedDependencyNode): List<String> {
    if (currentNode in visitedNodes) {
      return listOf("$currentNode (*)")
    }
    visitedNodes.add(currentNode)

    val childrenLines = arrayListOf<List<String>>()

    graph.missingDependencies
      .getOrDefault(currentNode, emptySet())
      .sortedBy { it.dependency.id }.mapTo(childrenLines) { missingDependency ->
        listOf("(failed) ${missingDependency.dependency}: ${missingDependency.missingReason}")
      }

    val directEdges = graph.getEdgesFrom(currentNode)
      .sortedWith(
        compareBy<ResolvedDependencyEdge> { if (it.dependency.isOptional) 1 else -1 }
          .thenBy { if (it.dependency.isModule) 1 else -1 }
          .thenBy { it.dependency.id }
          .thenBy { it.to.id }
          .thenBy { it.to.version }
      )

    for (edge in directEdges) {
      val childLines = recursivelyCalculateLines(edge.to)
      val headerLine = buildString {
        if (edge.dependency.isOptional) {
          append("(optional) ")
        }
        append(childLines.first())
        if (edge.to.isProductModule) {
          append(" [product module]")
        } else if (edge.dependency.isModule) {
          append(" [declaring module ${edge.dependency.id}]")
        }
      }
      val tailLines = childLines.drop(1)
      childrenLines.add(listOf(headerLine) + tailLines)
    }

    val result = arrayListOf<String>()
    result += currentNode.toString()

    if (childrenLines.isNotEmpty()) {
      val headingChildren = childrenLines.dropLast(1)
      val lastChild = childrenLines.last()

      if (headingChildren.isNotEmpty()) {
        for (headingChild in headingChildren) {
          val firstLine = headingChild.first().let { "+--- $it" }
          val tailLines = headingChild.drop(1).map { "|    $it" }
          result += firstLine
          result += tailLines
        }
      }

      result += lastChild.first().let { "\\--- $it" }
      result += lastChild.drop(1).map { "     $it" }
    }

    return result
  }
}

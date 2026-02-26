/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.presentation

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode

/**
 * Provides the [prettyPresentation] method that prints the [DependenciesGraph] in
 * a fancy way like the 'gradle dependencies' does:
 *
 * ```
 * start:1.0
 * +--- b:1.0
 * |    +--- c:1.0
 * |    |    +--- (failed) e: plugin e is not found
 * |    |    +--- (failed) f (optional): plugin e is not found
 * |    |    \--- (optional) optional.module:IU-181.1 [declaring module optional.module]
 * |    \--- some.module:IU-181.1 [declaring module some.module]
 * \--- c:1.0 (*)
 * ```
 */
class DependenciesGraphPrettyPrinter(private val dependenciesGraph: DependenciesGraph) {

  private val visitedNodes = hashSetOf<DependencyNode>()

  fun prettyPresentation(): String =
    recursivelyCalculateLines(dependenciesGraph.verifiedPlugin).joinToString(separator = "\n")

  private fun recursivelyCalculateLines(currentNode: DependencyNode): List<String> {
    if (currentNode in visitedNodes) {
      //This node has already been printed with all its dependencies.
      return listOf("$currentNode (*)")
    }
    visitedNodes.add(currentNode)

    val childrenLines = arrayListOf<List<String>>()

    dependenciesGraph.missingDependencies
      .getOrDefault(currentNode, emptySet())
      .sortedBy { it.dependency.id }.mapTo(childrenLines) { missingDependency ->
        listOf("(failed) ${missingDependency.dependency}: ${missingDependency.missingReason}")
      }

    val directEdges = dependenciesGraph.getEdgesFrom(currentNode)
      .sortedWith(
        compareBy<DependencyEdge> { if (it.dependency.isOptional) 1 else -1 }
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
        if (edge.dependency.isModule) {
          append(" [declaring module ${edge.dependency.id}]")
        }
      }
      val tailLines = childLines.drop(1)
      childrenLines.add(listOf(headerLine) + tailLines)
    }

    val result = arrayListOf<String>()
    result += "${currentNode.id}:${currentNode.version}"

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
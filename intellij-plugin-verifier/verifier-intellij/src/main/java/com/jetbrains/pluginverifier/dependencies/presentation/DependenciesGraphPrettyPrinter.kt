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

  private companion object {
    const val NOT_LAST_DEPENDENCY_FIRST_LINE_PREFIX = "+--- "

    const val NOT_LAST_DEPENDENCY_INTERMEDIATE_LINE_PREFIX = "|    "

    const val LAST_DEPENDENCY_FIRST_LINE_PREFIX = "\\--- "

    const val LAST_DEPENDENCY_INTERMEDIATE_LINE_PREFIX = "     "

    const val TRANSITIVE_DEPENDENCY_SUFFIX = "(*)"

    const val FAILED_DEPENDENCY_PREFIX = "(failed)"

    const val OPTIONAL_DEPENDENCY_PREFIX = "(optional)"
  }

  private val visitedNodes = hashSetOf<DependencyNode>()

  fun prettyPresentation(): String =
      recursivelyCalculateLines(dependenciesGraph.verifiedPlugin).joinToString(separator = "\n")

  private fun recursivelyCalculateLines(currentNode: DependencyNode): List<String> {
    if (currentNode in visitedNodes) {
      //This node has already been printed with all its dependencies.
      return listOf("$currentNode $TRANSITIVE_DEPENDENCY_SUFFIX")
    }
    visitedNodes.add(currentNode)

    val childrenLines = arrayListOf<List<String>>()

    currentNode.missingDependencies.sortedBy { it.dependency.id }.mapTo(childrenLines) { missingDependency ->
      listOf("$FAILED_DEPENDENCY_PREFIX ${missingDependency.dependency}: ${missingDependency.missingReason}")
    }

    val directEdges = dependenciesGraph.getEdgesFrom(currentNode)
        .sortedWith(
            compareBy<DependencyEdge> { if (it.dependency.isOptional) -1 else 1 }
                .thenBy { if (it.dependency.isModule) -1 else 1 }
                .thenBy { it.to.pluginId }
                .thenBy { it.to.version }
                .thenBy { it.dependency.id }
        )

    for (edge in directEdges) {
      val childLines = recursivelyCalculateLines(edge.to)
      val headerLine = buildString {
        if (edge.dependency.isOptional) {
          append("$OPTIONAL_DEPENDENCY_PREFIX ")
        }
        append(childLines.first())
        if (edge.dependency.isModule) {
          append(" " + "[declaring module ${edge.dependency.id}]")
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
          val firstLine = headingChild.first().let { NOT_LAST_DEPENDENCY_FIRST_LINE_PREFIX + it }
          val tailLines = headingChild.drop(1).map { NOT_LAST_DEPENDENCY_INTERMEDIATE_LINE_PREFIX + it }
          result += firstLine
          result += tailLines
        }
      }

      val lastChildFirstLine = lastChild.first().let { LAST_DEPENDENCY_FIRST_LINE_PREFIX + it }
      val lastChildTailLines = lastChild.drop(1).map { LAST_DEPENDENCY_INTERMEDIATE_LINE_PREFIX + it }
      result += lastChildFirstLine
      result += lastChildTailLines
    }

    return result
  }
}
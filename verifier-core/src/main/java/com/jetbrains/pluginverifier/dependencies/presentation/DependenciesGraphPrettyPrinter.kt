package com.jetbrains.pluginverifier.dependencies.presentation

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode

/**
 * Provides the [prettyPresentation] method that prints the [DependenciesGraph] in
 * a fancy way like the 'gradle dependencies' does:
 *
 * ```
 * start:1.0
 * +--- b:1.0
 * |    +--- c:1.0
 * |    |    +--- (optional) optional.module:<unspecified> [declaring module optional.module]
 * |    |    +--- (failed) e: plugin e is not found
 * |    |    \--- (failed) f (optional): plugin e is not found
 * |    \--- some.module:<unspecified> [declaring module some.module]
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

  fun prettyPresentation(): String {
    return recursivelyCalculateLines(dependenciesGraph.verifiedPlugin).joinToString(separator = "\n")
  }

  private fun recursivelyCalculateLines(currentNode: DependencyNode): List<String> {
    if (currentNode in visitedNodes) {
      //This node has already been printed with all its dependencies.
      return listOf("$currentNode $TRANSITIVE_DEPENDENCY_SUFFIX")
    }
    visitedNodes.add(currentNode)
    val result = arrayListOf<String>()
    result.add(currentNode.toString())

    val childrenLines = arrayListOf<List<String>>()

    val directEdges = dependenciesGraph.edges.filter { it.from == currentNode }
    for (edge in directEdges) {
      val childLines = recursivelyCalculateLines(edge.to)
      val headerLine = childLines.first().let { line ->
        buildString {
          if (edge.dependency.isOptional) {
            append(OPTIONAL_DEPENDENCY_PREFIX + " ")
          }
          append(line)
          if (edge.dependency.isModule) {
            append(" " + "[declaring module ${edge.dependency.id}]")
          }
        }
      }
      val tailLines = childLines.drop(1)
      childrenLines.add(listOf(headerLine) + tailLines)
    }

    currentNode.missingDependencies.mapTo(childrenLines) { missingDependency ->
      listOf("$FAILED_DEPENDENCY_PREFIX ${missingDependency.dependency}: ${missingDependency.missingReason}")
    }

    if (childrenLines.isNotEmpty()) {
      val headingChildren = childrenLines.dropLast(1)
      val lastChild = childrenLines.last()

      if (headingChildren.isNotEmpty()) {
        for (headingChild in headingChildren) {
          val firstLine = headingChild.first().let { NOT_LAST_DEPENDENCY_FIRST_LINE_PREFIX + it }
          val tailLines = headingChild.drop(1).map { NOT_LAST_DEPENDENCY_INTERMEDIATE_LINE_PREFIX + it }
          result.add(firstLine)
          result.addAll(tailLines)
        }
      }

      val lastChildFirstLine = lastChild.first().let { LAST_DEPENDENCY_FIRST_LINE_PREFIX + it }
      val lastChildTailLines = lastChild.drop(1).map { LAST_DEPENDENCY_INTERMEDIATE_LINE_PREFIX + it }
      result.add(lastChildFirstLine)
      result.addAll(lastChildTailLines)
    }

    return result
  }
}
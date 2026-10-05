/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.presentation

import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginAware
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.id
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
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

    data class ChildPresentation(val lines: List<String>, val isContentModule: Boolean = false)

    val childrenLines = arrayListOf<ChildPresentation>()

    dependenciesGraph.missingDependencies
      .getOrDefault(currentNode, emptySet())
      .sortedBy { it.dependency.id }.mapTo(childrenLines) { missingDependency ->
        ChildPresentation(
          listOf("(failed) ${missingDependency.dependency}: ${missingDependency.missingReason}"))
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
        if (edge.to is PluginAware && edge.to.plugin is IdeModule) {
          append(" [product module]")
        } else if (edge.to is DependencyNode.ContentModuleDeclaration) {
          append(" [declared as a content module]")
        } else if (edge.to is DependencyNode.ModuleDependency) {
          val moduleOwner = edge.to.plugin
          val version = edge.to.version
          append(" [content module declared in ${moduleOwner.id}:${version}]")
        } else if (edge.dependency.isModule) {
          append(" [declaring module ${edge.dependency.id}]")
        }
      }
      val tailLines = childLines.drop(1)
      childrenLines.add(ChildPresentation(
        lines = listOf(headerLine) + tailLines,
        isContentModule = edge.to is DependencyNode.ContentModuleDeclaration
      ))
    }

    val result = arrayListOf<String>()
    // First occurrence carries the aliases; repeated occurrences are printed as plain "id:version (*)".
    result += currentNode.toStringWithAliases()

    if (childrenLines.isNotEmpty()) {
      val headingChildren = childrenLines.dropLast(1)
      val lastChild = childrenLines.last()

      if (headingChildren.isNotEmpty()) {
        for (headingChild in headingChildren) {
          val connector = if (headingChild.isContentModule) "◆---" else "+---"
          val firstLine = headingChild.lines.first().let { "$connector $it" }
          val tailLines = headingChild.lines.drop(1).map { "|    $it" }
          result += firstLine
          result += tailLines
        }
      }

      val connector = if (lastChild.isContentModule) "◆---" else "\\---"
      result += lastChild.lines.first().let { "$connector $it" }
      result += lastChild.lines.drop(1).map { "     $it" }
    }

    return result
  }
}
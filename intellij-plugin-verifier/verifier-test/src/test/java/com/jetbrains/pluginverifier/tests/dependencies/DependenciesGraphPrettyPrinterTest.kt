/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.ide.PluginIdAndVersion
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.dependencies.DependencyNode.Companion.dependencyNode
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.dependencies.presentation.ResolvedDependenciesGraphPrettyPrinter
import org.junit.Assert
import org.junit.Test

/**
 * Tests the [DependenciesGraphPrettyPrinter].
 */
class DependenciesGraphPrettyPrinterTest {

  private val missingDependencies = hashMapOf<PluginIdAndVersion, MutableList<MissingDependency>>()

  private val dependencies = hashMapOf<Pair<PluginIdAndVersion, PluginIdAndVersion>, MutableList<PluginDependency>>()

  private fun parsePluginIdAndVersion(description: String): PluginIdAndVersion {
    require(":" in description)
    val (pluginId, version) = description.split(":")
    return PluginIdAndVersion(pluginId, version)
  }

  private fun parseFromAndTo(edgeDescription: String): Pair<PluginIdAndVersion, PluginIdAndVersion> {
    require("[" !in edgeDescription)
    val (from, to) = edgeDescription.split(" -> ")
    return parsePluginIdAndVersion(from) to parsePluginIdAndVersion(to)
  }

  private fun parseDescription(edgeDescription: String) {
    val isOptional = "[optional]" in edgeDescription
    val (from, to) = parseFromAndTo(edgeDescription.substringAfterLast("]"))
    val moduleId = if ("[module:" in edgeDescription) {
      edgeDescription.substringAfter("[module:").substringBefore("]")
    } else {
      null
    }

    val pluginDependency = PluginDependencyImpl(
      moduleId ?: to.pluginId,
      isOptional,
      moduleId != null
    )

    missingDependencies.putIfAbsent(from, arrayListOf())
    if ("[failed]" in edgeDescription) {
      val reason = edgeDescription.substringAfter("reason:").substringBefore("]")
      val missingDependency = MissingDependency(pluginDependency, reason)
      missingDependencies[from]!!.add(missingDependency)
    } else {
      dependencies.getOrPut(from to to) { arrayListOf() }.add(pluginDependency)
    }
  }

  private fun createDependencyNode(pluginIdAndVersion: PluginIdAndVersion) =
    dependencyNode(pluginIdAndVersion.pluginId, pluginIdAndVersion.version)

  @Test
  fun `test pretty print`() {
    val edgesDescriptions = listOf(
      "start:1.0 -> b:1.0",
      "b:1.0 -> c:1.0",
      "[module:mandatory.module]b:1.0 -> d:1.0",
      "[module:optional.module][optional]c:1.0 -> d:1.0",
      "[module:optional.module.2][optional]c:1.0 -> d:1.0",
      "start:1.0 -> c:1.0",
      "[failed][reason:plugin e is not found]c:1.0 -> e:1.0",
      "[failed][reason:plugin f is not found][optional]c:1.0 -> f:1.0",
      "[optional]start:1.0 -> e:1.0",
      "[module:optional.module.3]start:1.0 -> g:1.0"
    )

    edgesDescriptions.forEach { edgeDescription -> parseDescription(edgeDescription) }

    val vertices = missingDependencies.keys.mapTo(LinkedHashSet()) { createDependencyNode(it) }
    val edges = dependencies.flatMapTo(LinkedHashSet()) { (fromAndTo, deps) ->
      deps.map {
        DependencyEdge(createDependencyNode(fromAndTo.first), createDependencyNode(fromAndTo.second), it)
      }
    }

    val startVertex = vertices.find { it.id == "start" }!!
    val missingDeps: Map<DependencyNode, Set<MissingDependency>> = missingDependencies.map { (idAndVersion, deps) ->
      val (id, version) = idAndVersion
      dependencyNode(id, version) to deps.toSet()
    }.toMap()
    val dependenciesGraph = DependenciesGraph(startVertex, vertices, edges, missingDeps)
    val prettyPresentation = ResolvedDependenciesGraphPrettyPrinter(dependenciesGraph.toResolved()).prettyPresentation().trim()
    Assert.assertEquals(DependenciesGraphPrettyPrinter(dependenciesGraph).prettyPresentation().trim(), prettyPresentation)

    Assert.assertEquals(
      """
start:1.0
+--- b:1.0
|    +--- c:1.0
|    |    +--- (failed) e: plugin e is not found
|    |    +--- (failed) f (optional): plugin f is not found
|    |    +--- (optional) d:1.0 [declaring module optional.module]
|    |    \--- (optional) d:1.0 (*) [declaring module optional.module.2]
|    \--- d:1.0 (*) [declaring module mandatory.module]
+--- c:1.0 (*)
+--- g:1.0 [declaring module optional.module.3]
\--- (optional) e:1.0
""".trim(), prettyPresentation
    )
  }

  @Test
  fun `plugin with two content modules where one depends on another shows dependencies`() {
    val graph = pluginWithCoreAndExtrasModuleWithExtrasDependingOnCoreFixture().graph

    assertPrettyPresentation(
      graph, """
        owner:1.0
        +--- owner:1.0/owner.core [declared as a content module]
        \--- owner:1.0/owner.extras [declared as a content module]
             \--- owner:1.0/owner.core [content module declared in owner:1.0]
      """.trimIndent()
    )
  }

  @Test
  fun `simple modular fixture report shows its content module ownership edge`() {
    val graph = simpleModularDependenciesFixture().graph

    assertPrettyPresentation(
      graph, """
      owner:1.0
      \--- owner:1.0/owner.core [declared as a content module]
      """.trimIndent()
    )
  }

  @Test
  fun `module-only report shows ownership sibling and transitive paths`() {
    val graph = modularDependenciesFixture(repeatMainDependencies = false).graph

    assertPrettyPresentation(graph, """
      owner:1.0
      +--- owner:1.0/owner.core [declared as a content module]
      |    \--- com.intellij:261.1 (aliased com.intellij.modules.platform) [declaring module com.intellij.modules.platform]
      \--- owner:1.0/owner.extra [declared as a content module]
           +--- bundled:2.0
           |    \--- transitive:3.0
           \--- owner:1.0/owner.core [content module declared in owner:1.0]
    """.trimIndent())
  }

  @Test
  fun `report retains module paths when main descriptor repeats their dependencies`() {
    val graph = modularDependenciesFixture(repeatMainDependencies = true).graph

    assertPrettyPresentation(graph, """
owner:1.0
+--- bundled:2.0
|    \--- transitive:3.0
+--- com.intellij:261.1 (aliased com.intellij.modules.platform) [declaring module com.intellij.modules.platform]
+--- owner:1.0/owner.core [declared as a content module]
|    \--- com.intellij:261.1 (*) [declaring module com.intellij.modules.platform]
\--- owner:1.0/owner.extra [declared as a content module]
     +--- bundled:2.0 (*)
     \--- owner:1.0/owner.core [content module declared in owner:1.0]
""".trim())
  }

  @Test
  fun `legacy aliases and product modules keep their plugin-only report presentation`() {
    assertPrettyPresentation(legacyDependenciesGraph(), """
legacy:1.0
+--- com.intellij:261.1 (aliased com.intellij.modules.platform) [declaring module com.intellij.modules.platform]
\--- com.intellij.modules.product:261.1 (aliased com.intellij.modules.product) [product module]
""".trim())
  }

  private fun assertPrettyPresentation(graph: DependenciesGraph, expected: String) {
    val fatPresentation = DependenciesGraphPrettyPrinter(graph).prettyPresentation()
    val resolvedPresentation = ResolvedDependenciesGraphPrettyPrinter(graph.toResolved()).prettyPresentation()
    Assert.assertEquals(expected, fatPresentation)
    Assert.assertEquals(fatPresentation, resolvedPresentation)
  }
}
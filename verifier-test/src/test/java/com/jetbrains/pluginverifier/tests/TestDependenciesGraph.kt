package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.dependencies.*
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class TestDependenciesGraph {

  companion object {
    lateinit var SIMPLE_GRAPH_WITH_NO_CYCLES: DependenciesGraph
    lateinit var GRAPH_WITH_A_CYCLE: DependenciesGraph

    val a = node("A", "1", emptyList())
    val b = node("B", "2", emptyList())
    val c = node("C", "3", emptyList())
    val d = node("D", "4", listOf(missing("E", true, false, "broken download url")))

    @BeforeClass
    @JvmStatic
    fun initGraphs() {
      SIMPLE_GRAPH_WITH_NO_CYCLES = DependenciesGraph(
          a,
          listOf(a, b, c, d),
          listOf(
              dependencyEdge(a, b, "B", false, false),
              dependencyEdge(a, c, "C", false, false),
              dependencyEdge(b, d, "D", false, false),
              dependencyEdge(c, d, "D", false, false)
          )
      )

      GRAPH_WITH_A_CYCLE = SIMPLE_GRAPH_WITH_NO_CYCLES.copy(edges = this.SIMPLE_GRAPH_WITH_NO_CYCLES.edges + dependencyEdge(d, a, "A", false, false))
    }

    fun missing(missingId: String, isOptional: Boolean, isModule: Boolean, reason: String) = MissingDependency(PluginDependencyImpl(missingId, isOptional, isModule), reason)

    fun dependencyEdge(from: DependencyNode, to: DependencyNode, depId: String, isOptional: Boolean, isModule: Boolean) = DependencyEdge(from, to, PluginDependencyImpl(depId, isOptional, isModule))

    fun node(id: String, version: String, missings: List<MissingDependency>): DependencyNode = DependencyNode(id, version, missings)
  }

  @Test
  fun testNoCycles() {
    assertTrue(SIMPLE_GRAPH_WITH_NO_CYCLES.getCycles().isEmpty())
  }

  @Test
  fun testAtLeastOnePathToMissingDependency() {
    val paths = SIMPLE_GRAPH_WITH_NO_CYCLES.getMissingDependencyPaths()
    assertTrue(paths.isNotEmpty())
    val one = MissingDependencyPath(listOf(a, b, d), missing("E", true, false, "broken download url"))
    val two = MissingDependencyPath(listOf(a, c, d), missing("E", true, false, "broken download url"))
    assertTrue(paths.contains(one) || paths.contains(two))
  }

  @Test
  fun testCycle() {
    val cycles = GRAPH_WITH_A_CYCLE.getCycles()
    assertTrue(cycles.isNotEmpty())
    assertTrue(listOf(a, c, d) == cycles[0] || listOf(a, b, d) == cycles[0])
  }
}
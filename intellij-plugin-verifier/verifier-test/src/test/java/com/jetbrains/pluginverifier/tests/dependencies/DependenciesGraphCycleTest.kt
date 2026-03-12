/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.plugin.PluginV1Dependency
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import org.junit.Assert.assertEquals
import org.junit.Test

class DependenciesGraphCycleTest {

  /**
   * Suppose the dependencies are as follows:
   * a:1.0 -> b:1.0
   * b:1.0 -> c:1.0
   * c:1.0 -> b:1.0
   * b:1.0 -> d:1.0
   * d:1.0 -> a:1.0
   *
   * Note cycles (b -> c -> b) and (a -> b -> d -> a)
   *
   * The cycle (b -> c -> b) must not be reported as the verified plugin 'a' doesn't
   * belong to it. Only the (a -> b -> d -> a) must be reported.
   */
  @Test
  fun `only cycles containing the verified plugin should be reported`() {
    val a = DependencyNode("a", "1.0")
    val b = DependencyNode("b", "1.0")
    val c = DependencyNode("c", "1.0")
    val d = DependencyNode("d", "1.0")

    val dependenciesGraph = DependenciesGraph(
      a,
      setOf(a, b, c, d),
      setOf(
        DependencyEdge(a, b, PluginDependencyImpl("b", false, false)),
        DependencyEdge(b, c, PluginDependencyImpl("c", false, false)),
        DependencyEdge(c, b, PluginDependencyImpl("b", false, false)),
        DependencyEdge(b, d, PluginDependencyImpl("d", false, false)),
        DependencyEdge(d, a, PluginDependencyImpl("a", false, false))
      ),
      emptyMap()
    )

    val cycles = dependenciesGraph.getAllCyclesWithVerifiedPlugin()
    assertEquals(listOf(listOf(a, b, d)), cycles)
  }

  @Test
  fun `three-node cycle is detected`() {
    val a = DependencyNode("a", "1.0")
    val b = DependencyNode("b", "1.0")
    val c = DependencyNode("c", "1.0")

    val dependenciesGraph = DependenciesGraph(
      a,
      setOf(a, b, c),
      setOf(
        DependencyEdge(a, b, PluginV1Dependency.Mandatory ("b")),
        DependencyEdge(b, c, PluginV1Dependency.Mandatory("c")),
        DependencyEdge(c, a, PluginV1Dependency.Mandatory("a")),
      ),
      emptyMap()
    )

    val cycles = dependenciesGraph.getAllCyclesWithVerifiedPlugin()
    assertEquals(listOf(listOf(a, b, c)), cycles)
  }

  @Test
  fun `two-node cycle is detected`() {
    val a = DependencyNode("a", "1.0")
    val b = DependencyNode("b", "1.0")

    val dependenciesGraph = DependenciesGraph(
      a,
      setOf(a, b),
      setOf(
        DependencyEdge(a, b, PluginV1Dependency.Mandatory ("b")),
        DependencyEdge(b, a, PluginV1Dependency.Mandatory("a")),
      ),
      emptyMap()
    )

    val cycles = dependenciesGraph.getAllCyclesWithVerifiedPlugin()
    assertEquals(listOf(listOf(a, b)), cycles)
  }
}
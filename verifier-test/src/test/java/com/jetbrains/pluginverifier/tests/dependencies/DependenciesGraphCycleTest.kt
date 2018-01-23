package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import org.junit.Assert
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
    val a = DependencyNode("a", "1.0", emptyList())
    val b = DependencyNode("b", "1.0", emptyList())
    val c = DependencyNode("c", "1.0", emptyList())
    val d = DependencyNode("d", "1.0", emptyList())

    val dependenciesGraph = DependenciesGraph(
        a,
        listOf(a, b, c, d),
        listOf(
            DependencyEdge(a, b, PluginDependencyImpl("b", false, false)),
            DependencyEdge(b, c, PluginDependencyImpl("c", false, false)),
            DependencyEdge(c, b, PluginDependencyImpl("b", false, false)),
            DependencyEdge(b, d, PluginDependencyImpl("d", false, false)),
            DependencyEdge(d, a, PluginDependencyImpl("a", false, false))
        )
    )

    val allCycles = dependenciesGraph.getAllCycles()
    Assert.assertEquals(listOf(listOf(a, b, d)), allCycles)
  }


}
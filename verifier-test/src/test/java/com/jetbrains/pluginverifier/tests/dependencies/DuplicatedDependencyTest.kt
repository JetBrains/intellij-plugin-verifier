package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.ResultHolder
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.results.structure.PluginStructureWarning
import com.jetbrains.pluginverifier.results.warnings.DuplicatedDependencyWarning
import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicatedDependencyTest {

  /**
   * Suppose the dependency is duplicated as following:
   * a:1.0 -> b:1.0
   * a:1.0 -> b:1.0 (optional)
   */
  @Test
  fun `duplicated dependency`() {
    val a = DependencyNode("a", "1.0", emptyList())
    val b = DependencyNode("b", "1.0", emptyList())

    val dependenciesGraph = DependenciesGraph(
        a,
        listOf(a, b),
        listOf(
            DependencyEdge(a, b, PluginDependencyImpl("b", false, false)),
            DependencyEdge(a, b, PluginDependencyImpl("b", true, false))
        )
    )

    val resultHolder = ResultHolder()
    resultHolder.addDependenciesWarnings(dependenciesGraph)
    assertEquals(setOf(PluginStructureWarning(DuplicatedDependencyWarning(a, "b").message)), resultHolder.pluginStructureWarnings)
  }

}
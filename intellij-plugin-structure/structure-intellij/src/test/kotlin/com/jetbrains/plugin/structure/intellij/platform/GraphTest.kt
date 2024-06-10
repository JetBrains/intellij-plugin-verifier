package com.jetbrains.plugin.structure.intellij.platform

import com.jetbrains.plugin.structure.intellij.utils.Graph
import org.junit.Assert.assertEquals
import org.junit.Test

class GraphTest {
  @Test
  fun `simple graph is constructed`() {
    val g = Graph<String>().apply {
      addEdge("1", "2")
      addEdge("1", "3")
      addEdge("2", "4")
      addEdge("3", "4")
    }

    assertEquals("1 -> [2, 3];2 -> [4];3 -> [4];", g.toString())
  }

  @Test
  fun `simple graph is constructed via operator`() {
    val g = Graph<Int>()
    g += 1 to 2
    g += 1 to 3
    g += 2 to 4
    g += 3 to 4

    assertEquals("1 -> [2, 3];2 -> [4];3 -> [4];", g.toString())
  }
}
package com.jetbrains.pluginverifier.tests

import com.google.common.collect.Lists
import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.impl.utils.StringUtil
import com.jetbrains.pluginverifier.utils.DefaultDependencyResolver
import com.jetbrains.pluginverifier.utils.Dependencies
import com.jetbrains.pluginverifier.utils.Edge
import com.jetbrains.pluginverifier.utils.Vertex
import org.jgrapht.DirectedGraph
import org.jgrapht.traverse.DepthFirstIterator
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * @author Sergey Patrikeev
 */
class TestDependencies {

  private val dependencies = Arrays.asList<String>("first:1.0", "somePlugin:1.0", "moduleContainer:1.0")

  @Test
  @Throws(Exception::class)
  fun getDependenciesWithTransitive() {
    //defined IDEA plugins
    val firstPlugin = MockUtil.createMockPlugin("first", "1.0", listOf(PluginDependencyImpl("someModule", false)), listOf(PluginDependencyImpl("somePlugin", false)))
    val somePlugin = MockUtil.createMockPlugin("somePlugin", "1.0")
    val moduleContainer = MockUtil.createMockPlugin("moduleContainer", "1.0", definedModules = setOf("someModule"))

    val ide = MockUtil.createTestIde(IdeVersion.createIdeVersion("IU-144"), listOf(firstPlugin, somePlugin, moduleContainer))
    val plugin = MockUtil.createMockPlugin("myPlugin", "1.0", emptyList(), listOf(PluginDependencyImpl("first", true)))

    val (graph, vertex) = Dependencies.calcDependencies(plugin, DefaultDependencyResolver(ide))

    val deps = graph.getTransitiveDependencies(vertex).map { it.plugin }

    assertNotNull(deps)
    assertEquals("Missing transitive dependencies", dependencies.size, deps.size)

    for (s in dependencies) {
      var found = false
      for (dep in deps) {
        if (StringUtil.equal(s.split((":").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0], dep.pluginId)) {
          found = true
        }
      }
      assertTrue("Dependency $s is not found", found)
    }
  }

  private fun DirectedGraph<Vertex, Edge>.getTransitiveDependencies(start: Vertex): List<Vertex> {
    val iterator = DepthFirstIterator(this, start)
    if (!iterator.hasNext()) return emptyList()
    iterator.next() //skip the start
    return Lists.newArrayList(iterator)
  }

}

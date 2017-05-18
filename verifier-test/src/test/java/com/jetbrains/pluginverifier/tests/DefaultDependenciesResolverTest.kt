package com.jetbrains.pluginverifier.tests

import com.google.common.collect.Lists
import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.impl.domain.PluginCreationSuccessImpl
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.plugin.Plugin
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.DepEdge
import com.jetbrains.pluginverifier.dependencies.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.DepVertex
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.tests.MockUtil.createMockPlugin
import com.jetbrains.pluginverifier.utils.DefaultDependencyResolver
import org.jgrapht.DirectedGraph
import org.jgrapht.traverse.DepthFirstIterator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class DefaultDependenciesResolverTest {

  @Test
  fun `get all plugin transitive dependencies`() {
    /*
    Given following dependencies between plugins:

    `test` -> `someModule` (defined in `moduleContainer`)
    `test` -> `somePlugin`

    `myPlugin` -> `test`

    Should find dependencies on `test`, `somePlugin` and `moduleContainer`
     */
    val testPlugin = createMockPlugin("test", "1.0", listOf(PluginDependencyImpl("someModule", false)), listOf(PluginDependencyImpl("somePlugin", false)))
    val somePlugin = createMockPlugin("somePlugin", "1.0")
    val moduleContainer = createMockPlugin("moduleContainer", "1.0", definedModules = setOf("someModule"))

    val ide = MockUtil.createMockIde(IdeVersion.createIdeVersion("IU-144"), listOf(testPlugin, somePlugin, moduleContainer))

    val plugin = createMockPlugin("myPlugin", "1.0", emptyList(), listOf(PluginDependencyImpl("test", true)))

    val success = CreatePluginResult.OK(PluginCreationSuccessImpl(plugin, emptyList()), Resolver.getEmptyResolver())
    val (graph, vertex) = DepGraphBuilder(DefaultDependencyResolver(ide)).build(success)

    val deps: List<Plugin> = getTransitiveDependencies(graph, vertex).map { it.creationOk.success.plugin }
    assertEquals(deps.map { it.pluginId }.toSet(), setOf("test", "somePlugin", "moduleContainer"))
  }

  private fun getTransitiveDependencies(graph: DirectedGraph<DepVertex, DepEdge>, start: DepVertex): List<DepVertex> {
    val iterator = DepthFirstIterator(graph, start)
    if (!iterator.hasNext()) return emptyList()
    iterator.next() //skip the start
    return Lists.newArrayList(iterator)
  }

}

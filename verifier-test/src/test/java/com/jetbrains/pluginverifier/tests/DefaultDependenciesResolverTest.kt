package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.IdeDependencyResolver
import com.jetbrains.pluginverifier.tests.MockUtil.createMockPlugin
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
    val testPlugin = createMockPlugin("test", "1.0", listOf(PluginDependencyImpl("someModule", false, true), PluginDependencyImpl("somePlugin", false, false)), emptySet())
    val somePlugin = createMockPlugin("somePlugin", "1.0", emptyList(), emptySet())
    val moduleContainer = createMockPlugin("moduleContainer", "1.0", emptyList(), setOf("someModule"))

    val ide = MockUtil.createMockIde(IdeVersion.createIdeVersion("IU-144"), listOf(testPlugin, somePlugin, moduleContainer))

    val plugin = createMockPlugin("myPlugin", "1.0", listOf(PluginDependencyImpl("test", true, false)), emptySet())

    val (graph, start) = DepGraphBuilder(IdeDependencyResolver(ide)).build(plugin, Resolver.getEmptyResolver())
    val dependenciesGraph = DepGraph2ApiGraphConverter.convert(graph, start)

    val deps: List<String> = dependenciesGraph.vertices.map { it.id }
    assertEquals(setOf("myPlugin", "test", "somePlugin", "moduleContainer"), deps.toSet())
  }

}

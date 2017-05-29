package com.jetbrains.pluginverifier.tests

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.dependencies.DefaultDependencyResolver
import com.jetbrains.pluginverifier.dependencies.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.DepGraphBuilder
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
    val testPlugin = createMockPlugin("test", "1.0", listOf(PluginDependencyImpl("someModule", false)), listOf(PluginDependencyImpl("somePlugin", false)))
    val somePlugin = createMockPlugin("somePlugin", "1.0")
    val moduleContainer = createMockPlugin("moduleContainer", "1.0", definedModules = setOf("someModule"))

    val ide = MockUtil.createMockIde(IdeVersion.createIdeVersion("IU-144"), listOf(testPlugin, somePlugin, moduleContainer))

    val plugin = createMockPlugin("myPlugin", "1.0", emptyList(), listOf(PluginDependencyImpl("test", true)))

    val (graph, start) = DepGraphBuilder(DefaultDependencyResolver(ide)).build(plugin, Resolver.getEmptyResolver())
    val dependenciesGraph = DepGraph2ApiGraphConverter.convert(graph, start)

    val deps: List<String> = dependenciesGraph.vertices.map { it.id }
    assertEquals(setOf("myPlugin", "test", "somePlugin", "moduleContainer"), deps.toSet())
  }

}

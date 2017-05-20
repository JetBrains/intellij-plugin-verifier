package com.jetbrains.pluginverifier.tests

import com.intellij.structure.ide.IdeVersion
import com.intellij.structure.impl.domain.PluginCreationSuccessImpl
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.*
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.ide.CreateIdeResult
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * @author Sergey Patrikeev
 */
class TestCheckPluginDependents {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `check-plugin command on two plugins with dependencies between them must not fail`() {
    //given
    val first = MockUtil.createMockPlugin("p1", "1", emptyList(), emptyList())
    val second = MockUtil.createMockPlugin("p2", "2", emptyList(), listOf(PluginDependencyImpl("p1", false)))
    val firstDescriptor = PluginDescriptor.ByInstance(CreatePluginResult.OK(PluginCreationSuccessImpl(first, emptyList()), Resolver.getEmptyResolver()))
    val secondDescriptor = PluginDescriptor.ByInstance(CreatePluginResult.OK(PluginCreationSuccessImpl(second, emptyList()), Resolver.getEmptyResolver()))

    val ide = MockUtil.createMockIde(IdeVersion.createIdeVersion("IU-163"), emptyList())
    val ideDescriptor = IdeDescriptor(CreateIdeResult(ide, Resolver.getEmptyResolver()))
    val jdkDescriptor = JdkDescriptor(temporaryFolder.newFolder())

    //when
    val params: CheckPluginParams = CheckPluginParams(listOf(firstDescriptor, secondDescriptor), listOf(ideDescriptor), jdkDescriptor, emptyList(), ProblemsFilter.AlwaysTrue)
    val configuration = CheckPluginConfiguration()
    val verify = configuration.execute(params)

    //then
    val secondPluginResult = verify.results[1]

    val dependenciesGraph = (secondPluginResult.verdict as Verdict.OK).dependenciesGraph

    val firstNode = DependencyNode("p1", "1", emptyList())
    val secondNode = DependencyNode("p2", "2", emptyList())
    val secondToFirstDep = PluginDependencyImpl("p1", false)
    val secondToFirstEdge = DependencyEdge(secondNode, firstNode, secondToFirstDep)

    Assert.assertEquals(secondNode, dependenciesGraph.start)
    Assert.assertEquals(listOf(secondNode, firstNode), dependenciesGraph.vertices)
    Assert.assertEquals(listOf(secondToFirstEdge), dependenciesGraph.edges)
  }
}
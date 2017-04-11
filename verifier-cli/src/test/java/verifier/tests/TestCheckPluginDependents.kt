package verifier.tests

import com.intellij.structure.domain.IdeVersion
import com.intellij.structure.impl.domain.PluginDependencyImpl
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.IdeDescriptor
import com.jetbrains.pluginverifier.api.PluginDescriptor
import com.jetbrains.pluginverifier.api.VOptions
import com.jetbrains.pluginverifier.api.VResult
import com.jetbrains.pluginverifier.configurations.CheckPluginConfiguration
import com.jetbrains.pluginverifier.configurations.CheckPluginParams
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import org.junit.Assert
import org.junit.Test

/**
 * @author Sergey Patrikeev
 */
class TestCheckPluginDependents {
  @Test
  fun `check-plugin command on two plugins with dependencies between them must not fail`() {
    //given
    val first = MockUtil.createMockPlugin("p1", "1", emptyList(), emptyList())
    val second = MockUtil.createMockPlugin("p2", "2", emptyList(), listOf(PluginDependencyImpl("p1", false)))
    val firstDescriptor = PluginDescriptor.ByInstance(first, Resolver.getEmptyResolver())
    val secondDescriptor = PluginDescriptor.ByInstance(second, Resolver.getEmptyResolver())

    val ide = MockUtil.createTestIde(IdeVersion.createIdeVersion("IU-163"), emptyList())
    val ideDescriptor = IdeDescriptor.ByInstance(ide, Resolver.getEmptyResolver())
    val jdkDescriptor = MockUtil.getJdkDescriptor()

    //when
    val params = CheckPluginParams(listOf(firstDescriptor, secondDescriptor), listOf(ideDescriptor), jdkDescriptor, VOptions())
    val configuration = CheckPluginConfiguration(params)
    val verify = configuration.execute()

    //then
    val results = verify.vResults.results
    val p2Result = results[1]

    val dependenciesGraph = (p2Result as VResult.Nice).dependenciesGraph

    val firstNode = DependencyNode("p1", "1", emptyMap())
    val secondNode = DependencyNode("p2", "2", emptyMap())
    val secondToFirstDep = PluginDependencyImpl("p1", false)
    val secondToFirstEdge = DependencyEdge(secondNode, firstNode, secondToFirstDep)

    Assert.assertEquals(secondNode, dependenciesGraph.start)
    Assert.assertEquals(listOf(secondNode, firstNode), dependenciesGraph.vertices)
    Assert.assertEquals(listOf(secondToFirstEdge), dependenciesGraph.edges)
  }
}
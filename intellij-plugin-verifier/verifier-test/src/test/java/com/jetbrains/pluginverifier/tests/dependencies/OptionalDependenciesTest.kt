package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.OptionalPluginDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependenciesGraphBuilder
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

/*
  Given the following structure of plugin.xml and optional dependencies' configuration files.

      plugin.xml
        <id>someId</id>
        <version>1.0</version>
        <depends optional="true" config-file="optionalPlugin.mxl">optionalPluginId</depends>
        <depends>duplicatedMandatoryDependencyId</depends>

      optionalPlugin.xml
        <depends>optionalMandatoryPluginId</depends>
        <depends optional="true" config-file="otherOptionalPlugin.mxl">otherOptionalPluginId</depends>

        <depends>missingMandatoryPluginId</depends>
        <depends optional="true" config-file="missingOptionalPlugin.mxl">missingOptionalPluginId</depends>

        <depends>duplicatedMandatoryDependencyId</depends>

      otherOptionalPlugin.xml
        <empty>

      missingOptionalPlugin.xml
        <empty>


  The test checks:
   1) "optionalMandatoryPluginId" becomes direct optional dependency of "someId" (because it is referenced via optional "optionalPlugin.xml").
   2) "otherOptionalPluginId" becomes direct optional dependency of "someId" (for the same reason).
   3) "missingMandatoryPluginId" becomes direct missing optional dependency of "someId" (for the same reason).
   4) "missingOptionalPluginId" becomes direct missing optional dependency of "someId" (for the same reason).
   5) "duplicatedMandatoryDependencyId" stays mandatory dependency of "someId", although it is optional via "optionalPlugin.xml"
*/
class OptionalDependenciesTest {

  @Test
  fun `test resolution of transitive dependencies from optional dependencies configuration files`() {
    //Plugin descriptor corresponding to "otherOptionalPlugin.xml".
    val otherOptionalPluginDescriptor = MockIdePlugin()

    //Plugin descriptor corresponding to "missingOptionalPlugin.xml".
    val missingOptionalPluginDescriptor = MockIdePlugin()

    //Plugin descriptor corresponding to "optionalPlugin.xml".
    val otherOptionalPluginId = PluginDependencyImpl("otherOptionalPluginId", true, false)
    val missingOptionalPluginId = PluginDependencyImpl("missingOptionalPluginId", true, false)
    val optionalMandatoryPluginId = PluginDependencyImpl("optionalMandatoryPluginId", false, false)
    val missingMandatoryPluginId = PluginDependencyImpl("missingMandatoryPluginId", false, false)
    val duplicatedMandatoryDependencyId = PluginDependencyImpl("duplicatedMandatoryDependencyId", false, false)

    val optionalPluginDescriptor = MockIdePlugin(
      dependencies = listOf(
        optionalMandatoryPluginId,
        otherOptionalPluginId,

        missingMandatoryPluginId,
        missingOptionalPluginId,

        duplicatedMandatoryDependencyId
      ),
      optionalDescriptors = listOf(
        OptionalPluginDescriptor(
          otherOptionalPluginId,
          otherOptionalPluginDescriptor,
          "otherOptionalPlugin.xml"
        ),
        OptionalPluginDescriptor(
          missingOptionalPluginId,
          missingOptionalPluginDescriptor,
          "missingOptionalPlugin.xml"
        )
      )
    )

    //Plugin descriptor corresponding to "plugin.xml"
    val optionalPluginId = PluginDependencyImpl("optionalPluginId", true, false)

    val somePluginDescriptor = MockIdePlugin(
      pluginId = "someId",
      pluginVersion = "1.0",
      dependencies = listOf(
        optionalPluginId,
        duplicatedMandatoryDependencyId
      ),
      optionalDescriptors = listOf(
        OptionalPluginDescriptor(
          optionalPluginId,
          optionalPluginDescriptor,
          "optionalPlugin.xml"
        )
      )
    )

    val dependencyFinder = object : DependencyFinder {
      override val presentableName: String
        get() = "test"

      override fun findPluginDependency(dependencyId: String, isModule: Boolean): DependencyFinder.Result {
        return when (dependencyId) {
          "optionalPluginId" -> DependencyFinder.Result.FoundPlugin(MockIdePlugin(pluginId = "optionalPluginId", pluginVersion = "1.0"))
          "otherOptionalPluginId" -> DependencyFinder.Result.FoundPlugin(MockIdePlugin(pluginId = "otherOptionalPluginId", pluginVersion = "1.0"))
          "optionalMandatoryPluginId" -> DependencyFinder.Result.FoundPlugin(MockIdePlugin(pluginId = "optionalMandatoryPluginId", pluginVersion = "1.0"))
          "duplicatedMandatoryDependencyId" -> DependencyFinder.Result.FoundPlugin(MockIdePlugin(pluginId = "duplicatedMandatoryDependencyId", pluginVersion = "1.0"))

          "missingMandatoryPluginId" -> DependencyFinder.Result.NotFound("missingMandatoryPluginId mandatory plugin is not found")
          "missingOptionalPluginId" -> DependencyFinder.Result.NotFound("missingOptionalPluginId optional plugin is not found")
          else -> throw IllegalArgumentException("Unknown test dependency $dependencyId")
        }
      }

      override fun findPluginDependency(dependency: PluginDependency): DependencyFinder.Result {
        return findPluginDependency(dependency.id, dependency.isModule)
      }
    }

    val ide = MockIde(IdeVersion.createIdeVersion("1.0"))


    //Build dependencies graph and compare it to what is expected.
    val (dependenciesGraph) = DependenciesGraphBuilder(dependencyFinder).buildDependenciesGraph(somePluginDescriptor, ide)
    val somePluginNode = DependencyNode("someId", "1.0")
    val optionalPluginNode = DependencyNode("optionalPluginId", "1.0")
    val optionalMandatoryPluginNode = DependencyNode("optionalMandatoryPluginId", "1.0")
    val otherOptionalPluginNode = DependencyNode("otherOptionalPluginId", "1.0")
    val duplicatedMandatoryPluginNode = DependencyNode("duplicatedMandatoryDependencyId", "1.0")

    assertEquals(somePluginNode, dependenciesGraph.verifiedPlugin)
    assertEquals(setOf(somePluginNode, optionalPluginNode, optionalMandatoryPluginNode, otherOptionalPluginNode, duplicatedMandatoryPluginNode), dependenciesGraph.vertices.toSet())

    dependenciesGraph.assertContainsEdge(DependencyEdge(somePluginNode, optionalPluginNode, PluginDependencyImpl("optionalPluginId", true, false)))

    //Mandatory dependency "optionalMandatoryPluginId" of file "optionalPlugin.xml" becomes optional.
    dependenciesGraph.assertContainsEdge(DependencyEdge(somePluginNode, optionalMandatoryPluginNode, PluginDependencyImpl("optionalMandatoryPluginId", true, false)))

    dependenciesGraph.assertContainsEdge(DependencyEdge(somePluginNode, otherOptionalPluginNode, PluginDependencyImpl("otherOptionalPluginId", true, false)))

    //Mandatory dependency "duplicatedMandatoryDependencyId" stays mandatory, even though it is optional via "optionalPlugin.xml".
    dependenciesGraph.assertContainsEdge(DependencyEdge(somePluginNode, duplicatedMandatoryPluginNode, PluginDependencyImpl("duplicatedMandatoryDependencyId", false, false)))

    assertEquals(4, dependenciesGraph.edges.size)

    assertEquals(
      mapOf(
        somePluginNode to setOf(
          //Mandatory dependency "missingMandatoryPluginId" of file "optionalPlugin.xml" becomes missing direct optional dependency of "somePlugin"
          MissingDependency(PluginDependencyImpl("missingMandatoryPluginId", true, false), "missingMandatoryPluginId mandatory plugin is not found"),
          MissingDependency(PluginDependencyImpl("missingOptionalPluginId", true, false), "missingOptionalPluginId optional plugin is not found")
        )
      ),
      dependenciesGraph.missingDependencies
    )
  }

  private fun DependenciesGraph.assertContainsEdge(edge: DependencyEdge) {
    if (edge !in edges) {
      Assert.fail("Graph must contain edge: '$edge' but contains\n" + edges.joinToString(separator = "\n") { "  $it" })
    }
  }
}
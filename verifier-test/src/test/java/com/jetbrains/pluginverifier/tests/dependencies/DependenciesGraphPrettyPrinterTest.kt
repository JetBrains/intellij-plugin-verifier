package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyEdge
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.presentation.DependenciesGraphPrettyPrinter
import com.jetbrains.pluginverifier.repository.PluginIdAndVersion
import org.junit.Assert
import org.junit.Test

/**
 * Tests the [DependenciesGraphPrettyPrinter].
 */
class DependenciesGraphPrettyPrinterTest {

  private val plugin2MissingDependencies = hashMapOf<PluginIdAndVersion, MutableList<MissingDependency>>()

  private val fromAndTo2Dependencies = hashMapOf<Pair<PluginIdAndVersion, PluginIdAndVersion>, PluginDependency>()

  private val ideVersion = IdeVersion.createIdeVersion("IU-181.1")

  private fun parsePluginIdAndVersion(description: String): PluginIdAndVersion {
    if (":" in description) {
      val (pluginId, version) = description.split(":")
      return PluginIdAndVersion(pluginId, version)
    }
    return PluginIdAndVersion(description, ideVersion.asString())
  }

  private fun parseFromAndTo(edgeDescription: String): Pair<PluginIdAndVersion, PluginIdAndVersion> {
    require("[" !in edgeDescription)
    val (from, to) = edgeDescription.split(" -> ")
    return parsePluginIdAndVersion(from) to parsePluginIdAndVersion(to)
  }

  private fun parseDescription(edgeDescription: String) {
    val isOptional = "[optional]" in edgeDescription
    val isModule = "[module]" in edgeDescription
    val (from, to) = parseFromAndTo(edgeDescription.substringAfterLast("]"))
    val pluginDependency = PluginDependencyImpl(to.pluginId, isOptional, isModule)
    plugin2MissingDependencies.putIfAbsent(from, arrayListOf())
    if ("[failed]" in edgeDescription) {
      val reason = edgeDescription.substringAfter("reason:").substringBefore("]")
      val missingDependency = MissingDependency(pluginDependency, reason)
      plugin2MissingDependencies[from]!!.add(missingDependency)
    } else {
      fromAndTo2Dependencies[from to to] = pluginDependency
    }
  }

  private fun createDependencyNode(pluginIdAndVersion: PluginIdAndVersion) =
      DependencyNode(pluginIdAndVersion.pluginId, pluginIdAndVersion.version, plugin2MissingDependencies.getOrDefault(pluginIdAndVersion, arrayListOf()))

  @Test
  fun `test pretty print`() {
    val edgesDescriptions = listOf(
        "start:1.0 -> b:1.0",
        "b:1.0 -> c:1.0",
        "[module]b:1.0 -> some.module",
        "[module][optional]c:1.0 -> optional.module",
        "start:1.0 -> c:1.0",
        "[failed][reason:plugin e is not found]c:1.0 -> e",
        "[failed][reason:plugin e is not found][optional]c:1.0 -> f"
    )

    edgesDescriptions.forEach { edgeDescription -> parseDescription(edgeDescription) }

    val vertices = plugin2MissingDependencies.keys.map { createDependencyNode(it) }
    val edges = fromAndTo2Dependencies.map { (fromAndTo, deps) -> DependencyEdge(createDependencyNode(fromAndTo.first), createDependencyNode(fromAndTo.second), deps) }

    val startVertex = vertices.find { it.pluginId == "start" }!!
    val dependenciesGraph = DependenciesGraph(startVertex, vertices.toList(), edges)
    val prettyPrinter = DependenciesGraphPrettyPrinter(dependenciesGraph)
    val prettyPresentation = prettyPrinter.prettyPresentation().trim()

    Assert.assertEquals(
        """
start:1.0
+--- b:1.0
|    +--- c:1.0
|    |    +--- (optional) optional.module:IU-181.1 [declaring module optional.module]
|    |    +--- (failed) e: plugin e is not found
|    |    \--- (failed) f (optional): plugin e is not found
|    \--- some.module:IU-181.1 [declaring module some.module]
\--- c:1.0 (*)
""".trim(), prettyPresentation)
  }
}
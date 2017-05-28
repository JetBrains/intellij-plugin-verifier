package com.jetbrains.pluginverifier.dependencies

import com.intellij.structure.plugin.PluginDependency
import com.jetbrains.pluginverifier.dependency.DependencyResolver
import com.jetbrains.pluginverifier.misc.closeLogged
import com.jetbrains.pluginverifier.plugin.CreatePluginResult
import com.jetbrains.pluginverifier.plugin.PluginCreator
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable

data class DepEdge(val dependency: PluginDependency) : DefaultEdge()

data class DepVertex(val creationOk: CreatePluginResult.OK) {
  val missingDependencies: MutableList<MissingDependency> = arrayListOf()

  override fun equals(other: Any?): Boolean = other is DepVertex && creationOk.plugin == other.creationOk.plugin

  override fun hashCode(): Int = creationOk.plugin.hashCode()

}

class DepGraphBuilder(private val dependencyResolver: DependencyResolver) : Closeable {
  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(DepGraphBuilder::class.java)
  }

  data class Result(val graph: DirectedGraph<DepVertex, DepEdge>, val start: DepVertex)

  private val graph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)

  fun build(creationOk: CreatePluginResult.OK): Result {
    LOG.debug("Building dependencies graph for ${creationOk.plugin}")
    val copiedResultOk = PluginCreator.getNonCloseableOkResult(creationOk)
    val startVertex = DepVertex(copiedResultOk)
    traverseDependencies(startVertex)
    return Result(graph, startVertex)
  }

  override fun close() = graph.vertexSet().forEach { it.creationOk.closeLogged() }

  private fun findDependencyOrFillMissingReason(pluginDependency: PluginDependency, isModule: Boolean, current: DepVertex): DepVertex? =
      getAlreadyResolvedDependency(pluginDependency) ?: resolveDependency(current, isModule, pluginDependency)

  private fun getAlreadyResolvedDependency(pluginDependency: PluginDependency): DepVertex? = graph.vertexSet().find { pluginDependency.id == it.creationOk.plugin.pluginId }

  private fun resolveDependency(current: DepVertex, isModule: Boolean, pluginDependency: PluginDependency): DepVertex? {
    val resolved = dependencyResolver.resolve(pluginDependency.id, isModule, current.creationOk.plugin)
    return when (resolved) {
      is DependencyResolver.Result.FoundLocally -> DepVertex(resolved.pluginCreateOk)
      is DependencyResolver.Result.Downloaded -> DepVertex(resolved.pluginCreateOk)
      is DependencyResolver.Result.NotFound -> {
        current.missingDependencies.add(MissingDependency(pluginDependency, isModule, resolved.reason))
        null
      }
      DependencyResolver.Result.Skip -> null
      is DependencyResolver.Result.ProblematicDependency -> return null
    }
  }

  private fun traverseDependencies(current: DepVertex) {
    if (graph.containsVertex(current)) {
      return
    }
    graph.addVertex(current)
    val plugin = current.creationOk.plugin
    for (pluginDependency in plugin.moduleDependencies + plugin.dependencies) {
      val isModule = pluginDependency in plugin.moduleDependencies
      val dependency = findDependencyOrFillMissingReason(pluginDependency, isModule, current) ?: continue
      traverseDependencies(dependency)
      graph.addEdge(current, dependency, DepEdge(pluginDependency))
    }
  }

}
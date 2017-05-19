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

data class DepVertex(val creationOk: CreatePluginResult.OK) : Closeable {
  val missingDependencies: MutableList<MissingDependency> = arrayListOf()

  override fun equals(other: Any?): Boolean = other is DepVertex && creationOk.success.plugin == other.creationOk.success.plugin

  override fun hashCode(): Int = creationOk.success.plugin.hashCode()

  override fun close() = creationOk.close()
}

class DepGraphBuilder(private val dependencyResolver: DependencyResolver) {

  data class Result(val graph: DirectedGraph<DepVertex, DepEdge>, val start: DepVertex)

  private val graph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)

  companion object {
    private val LOG: Logger = LoggerFactory.getLogger(DepGraphBuilder::class.java)
  }

  fun build(creationOk: CreatePluginResult.OK): Result {
    LOG.debug("Building dependencies graph for ${creationOk.success.plugin}")
    val copiedResultOk = PluginCreator.getUncloseableOkResult(creationOk)
    val startVertex = DepVertex(copiedResultOk)
    try {
      traverseDependencies(startVertex)
      return Result(graph, startVertex)
    } catch (e: Throwable) {
      graph.vertexSet().forEach { it.closeLogged() }
      throw e
    }
  }

  private fun findDependencyOrFillMissingReason(pluginDependency: PluginDependency, isModule: Boolean, current: DepVertex): DepVertex? =
      getResolvedDependency(pluginDependency) ?: resolveDependency(current, isModule, pluginDependency)

  private fun getResolvedDependency(pluginDependency: PluginDependency): DepVertex? = graph.vertexSet().find { pluginDependency.id == it.creationOk.success.plugin.pluginId }

  private fun resolveDependency(current: DepVertex, isModule: Boolean, pluginDependency: PluginDependency): DepVertex? {
    val resolved = dependencyResolver.resolve(pluginDependency.id, isModule, current.creationOk.success.plugin)
    return when (resolved) {
      is DependencyResolver.Result.Found -> DepVertex(resolved.pluginCreateOk)
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
    val plugin = current.creationOk.success.plugin
    for (pluginDependency in plugin.moduleDependencies + plugin.dependencies) {
      val isModule = pluginDependency in plugin.moduleDependencies
      val dependency = findDependencyOrFillMissingReason(pluginDependency, isModule, current) ?: continue
      traverseDependencies(dependency)
      graph.addEdge(current, dependency, DepEdge(pluginDependency))
    }
  }

}
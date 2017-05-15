package com.jetbrains.pluginverifier.dependencies

import com.intellij.structure.domain.Plugin
import com.intellij.structure.domain.PluginDependency
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.DependencyResolver
import com.jetbrains.pluginverifier.repository.FileLock
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

data class DepEdge(val dependency: PluginDependency) : DefaultEdge()

data class DepVertex(val plugin: Plugin, val resolver: Resolver, val fileLock: FileLock?) {

  val missingDependencies: MutableList<MissingDependency> = arrayListOf()

}

class DepGraphBuilder(private val dependencyResolver: DependencyResolver) {

  data class Result(val graph: DirectedGraph<DepVertex, DepEdge>, val start: DepVertex)

  private val graph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)

  fun build(plugin: Plugin, pluginResolver: Resolver): Result {
    val startVertex = DepVertex(plugin, pluginResolver, null)
    try {
      traverseDependencies(startVertex)
      return Result(graph, startVertex)
    } catch (e: Throwable) {
      graph.vertexSet().mapNotNull { it.fileLock }.forEach { it.release() }
      throw e
    }
  }

  private fun findDependencyOrFillMissingReason(pluginDependency: PluginDependency, isModule: Boolean, current: DepVertex): DepVertex? =
      getResolvedDependency(pluginDependency) ?: resolveDependency(current, isModule, pluginDependency)

  private fun getResolvedDependency(pluginDependency: PluginDependency): DepVertex? = graph.vertexSet().find { pluginDependency.id == it.plugin.pluginId }

  private fun resolveDependency(current: DepVertex, isModule: Boolean, pluginDependency: PluginDependency): DepVertex? {
    val resolved = dependencyResolver.resolve(pluginDependency.id, isModule, current.plugin)
    return when (resolved) {
      is DependencyResolver.Result.Found -> DepVertex(resolved.plugin, resolved.resolver, resolved.fileLock)
      is DependencyResolver.Result.NotFound -> {
        current.missingDependencies.add(MissingDependency(pluginDependency, isModule, resolved.reason))
        return null
      }
      DependencyResolver.Result.Skip -> {
        return null
      }
    }
  }

  private fun traverseDependencies(current: DepVertex) {
    if (graph.containsVertex(current)) {
      return
    }
    graph.addVertex(current)
    for (pluginDependency in current.plugin.moduleDependencies + current.plugin.dependencies) {
      val isModule = pluginDependency in current.plugin.moduleDependencies
      val dependency = findDependencyOrFillMissingReason(pluginDependency, isModule, current) ?: continue
      traverseDependencies(dependency)
      graph.addEdge(current, dependency, DepEdge(pluginDependency))
    }
  }

}
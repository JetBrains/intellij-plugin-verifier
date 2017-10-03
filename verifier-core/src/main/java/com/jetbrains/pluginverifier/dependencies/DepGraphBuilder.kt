package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.pluginverifier.misc.closeLogged
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.io.Closeable

data class DepEdge(val dependency: PluginDependency, val isModule: Boolean) : DefaultEdge() {
  public override fun getTarget(): Any = super.getTarget()

  public override fun getSource(): Any = super.getSource()
}

data class DepVertex(val id: String, val resolveResult: DependencyResolver.Result) : Closeable {
  override fun equals(other: Any?): Boolean = other is DepVertex && id == other.id

  override fun hashCode(): Int = id.hashCode()

  override fun close() = resolveResult.close()

}

class DepGraphBuilder(private val dependencyResolver: DependencyResolver) : Closeable {

  private val graph: DirectedGraph<DepVertex, DepEdge> = DefaultDirectedGraph(DepEdge::class.java)

  fun build(startPlugin: IdePlugin, startClassesLocations: IdePluginClassesLocations): Pair<DirectedGraph<DepVertex, DepEdge>, DepVertex> {
    val startResult = DependencyResolver.Result.FoundReady(startPlugin, startClassesLocations)
    val startVertex = DepVertex(startPlugin.pluginId ?: "", startResult)
    traverseDependencies(startVertex)
    return graph to startVertex
  }

  override fun close() = graph.vertexSet().forEach { it.closeLogged() }

  private fun findDependencyOrFillMissingReason(pluginDependency: PluginDependency): DepVertex? =
      getAlreadyResolvedDependency(pluginDependency) ?: resolveDependency(pluginDependency)

  private fun getAlreadyResolvedDependency(pluginDependency: PluginDependency): DepVertex? =
      graph.vertexSet().find { pluginDependency.id == it.id }

  private fun resolveDependency(pluginDependency: PluginDependency): DepVertex {
    val resolved = dependencyResolver.resolve(pluginDependency)
    return DepVertex(pluginDependency.id, resolved)
  }

  private fun getPlugin(resolveResult: DependencyResolver.Result): IdePlugin? = when (resolveResult) {
    is DependencyResolver.Result.FoundReady -> resolveResult.plugin
    is DependencyResolver.Result.CreatedResolver -> resolveResult.plugin
    is DependencyResolver.Result.Downloaded -> resolveResult.plugin
    is DependencyResolver.Result.ProblematicDependency -> null
    is DependencyResolver.Result.NotFound -> null
    is DependencyResolver.Result.FailedToDownload -> null
    DependencyResolver.Result.Skip -> null
  }

  private fun traverseDependencies(current: DepVertex) {
    if (graph.containsVertex(current)) {
      return
    }
    graph.addVertex(current)
    val plugin = getPlugin(current.resolveResult) ?: return
    for (pluginDependency in plugin.dependencies) {
      val isModule = pluginDependency.isModule
      val dependency = findDependencyOrFillMissingReason(pluginDependency) ?: continue
      traverseDependencies(dependency)
      graph.addEdge(current, dependency, DepEdge(pluginDependency, isModule))
    }
  }

}
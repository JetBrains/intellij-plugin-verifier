package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.checkIfInterrupted
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import org.jgrapht.DirectedGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

/**
 * Builds the dependencies graph using the [dependencyFinder].
 */
class DependenciesGraphBuilder(private val dependencyFinder: DependencyFinder) {

  private companion object {
    const val CORE_IDE_PLUGIN_ID = "com.intellij"
    const val JAVA_MODULE_ID = "com.intellij.modules.java"
    const val ALL_MODULES_ID = "com.intellij.modules.all"
  }

  fun buildDependenciesGraph(plugin: IdePlugin, ide: Ide): Pair<DependenciesGraph, List<DependencyFinder.Result>> {
    val depGraph = DefaultDirectedGraph<DepVertex, DepEdge>(DepEdge::class.java)

    val start = DepVertex(plugin.pluginId!!, DependencyFinder.Result.FoundPlugin(plugin))
    addTransitiveDependencies(depGraph, start)
    if (shouldAddImplicitDependenciesOnPlatformPlugins() && plugin.pluginId != CORE_IDE_PLUGIN_ID) {
      maybeAddOptionalJavaPluginDependency(plugin, depGraph,ide)
      maybeAddBundledPluginsWithUseIdeaClassLoader(depGraph, ide)
    }

    val dependenciesGraph = DepGraph2ApiGraphConverter(ide.version).convert(depGraph, start)
    return dependenciesGraph to depGraph.vertexSet().map { it.dependencyResult }
  }

  private fun addTransitiveDependencies(graph: DirectedGraph<DepVertex, DepEdge>, vertex: DepVertex) {
    checkIfInterrupted()
    if (!graph.containsVertex(vertex)) {
      graph.addVertex(vertex)
      val plugin = vertex.dependencyResult.getPlugin()
      if (plugin != null) {
        for (pluginDependency in plugin.dependencies) {
          val resolvedDependency = resolveDependency(pluginDependency, graph)
          addTransitiveDependencies(graph, resolvedDependency)

          /**
           * Skip the dependency onto itself.
           * An example of a plugin that declares a transitive dependency
           * on itself through modules dependencies is the 'IDEA CORE' plugin:
           *
           * PlatformLangPlugin.xml (declares module 'com.intellij.modules.lang') ->
           *   x-include /idea/RichPlatformPlugin.xml ->
           *   x-include /META-INF/DesignerCorePlugin.xml ->
           *   depends on module 'com.intellij.modules.lang'
           */
          if (vertex.pluginId != resolvedDependency.pluginId) {
            graph.addEdge(vertex, resolvedDependency, DepEdge(pluginDependency, vertex, resolvedDependency))
          }
        }
      }
    }
  }

  private fun DependencyFinder.Result.getPlugin() = when (this) {
    is DependencyFinder.Result.DetailsProvided -> when (pluginDetailsCacheResult) {
      is PluginDetailsCache.Result.Provided -> pluginDetailsCacheResult.pluginDetails.idePlugin
      is PluginDetailsCache.Result.InvalidPlugin -> null
      is PluginDetailsCache.Result.Failed -> null
      is PluginDetailsCache.Result.FileNotFound -> null
    }
    is DependencyFinder.Result.FoundPlugin -> plugin
    is DependencyFinder.Result.NotFound -> null
  }

  private fun resolveDependency(pluginDependency: PluginDependency, directedGraph: DirectedGraph<DepVertex, DepEdge>): DepVertex {
    val existingVertex = directedGraph.vertexSet().find { pluginDependency.id == it.pluginId }
    if (existingVertex != null) {
      return existingVertex
    }
    val dependencyResult = dependencyFinder.findPluginDependency(pluginDependency)
    return DepVertex(pluginDependency.id, dependencyResult)
  }

  /**
   * This option tells the verifier to supply Java and other platform plugins to the classpath of the verification.
   * It is necessary to avoid a lot of problems like "Access to unresolved class <some class from Java plugin>".
   *
   * Java plugin used to be part of the platform. But starting from 2019.2 EAP it is a separate bundled plugin.
   * Many plugins historically do not declare dependency onto Java plugin but require its classes.
   * The dependency may be declared either via module 'com.intellij.modules.java' or via plugin id 'com.intellij.java'.
   * IDE still loads Java plugin as part of the platform, using platform classloader, so plugins will continue to work.
   * But this behaviour may be changed in future. We are going to notify external developers that their plugins
   * reference Java-plugin classes without explicit dependency.
   *
   * [maybeAddOptionalJavaPluginDependency]
   * [maybeAddBundledPluginsWithUseIdeaClassLoader]
   */
  private fun shouldAddImplicitDependenciesOnPlatformPlugins() =
      System.getProperty("intellij.plugin.verifier.add.implicit.dependencies.on.platform.plugins") == "true"

  /**
   * If a plugin does not include any module dependency tags in its plugin.xml,
   * it is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA
   * https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/plugin_compatibility.html
   *
   * But since we've recently extracted Java to a separate plugin, many plugins may stop working
   * because they depend on Java plugin classes but do not explicitly declare a dependency onto 'com.intellij.modules.java'.
   *
   * So let's forcibly add Java as an optional dependency for such plugins.
   */
  private fun maybeAddOptionalJavaPluginDependency(plugin: IdePlugin, dependenciesGraph: DirectedGraph<DepVertex, DepEdge>, ide: Ide) {
    val isLegacyPlugin = plugin.dependencies.none { it.isModule }
    val isBundledPlugin = ide.bundledPlugins.any { it.pluginId == plugin.pluginId }
    val isCustomPlugin = !isBundledPlugin
    val doesIdeContainAllModules = ide.getPluginByModule(ALL_MODULES_ID) != null
    val shouldAddOptionalJavaPlugin = doesIdeContainAllModules && (isCustomPlugin || isLegacyPlugin)
    if (shouldAddOptionalJavaPlugin) {
      val javaModuleDependency = PluginDependencyImpl(JAVA_MODULE_ID, true, true)
      val dependencyResult = dependencyFinder.findPluginDependency(javaModuleDependency)
      val javaPluginId = when (dependencyResult) {
        is DependencyFinder.Result.DetailsProvided -> {
          val providedCacheEntry = dependencyResult.pluginDetailsCacheResult as? PluginDetailsCache.Result.Provided
          providedCacheEntry?.pluginDetails?.idePlugin?.pluginId
        }
        is DependencyFinder.Result.FoundPlugin -> dependencyResult.plugin.pluginId
        is DependencyFinder.Result.NotFound -> null
      } ?: return
      val javaPluginVertex = DepVertex(javaPluginId, dependencyResult)
      addTransitiveDependencies(dependenciesGraph, javaPluginVertex)
    }
  }

  /**
   * Bundled plugins that specify `<idea-plugin use-idea-classloader="true">` are automatically added to
   * platform class loader and may be referenced by other plugins without explicit dependency on them.
   *
   * We would like to emulate this behaviour by forcibly adding such plugins to the verification classpath.
   */
  private fun maybeAddBundledPluginsWithUseIdeaClassLoader(dependenciesGraph: DirectedGraph<DepVertex, DepEdge>, ide: Ide) {
    for (bundledPlugin in ide.bundledPlugins) {
      if (bundledPlugin.useIdeClassLoader && bundledPlugin.pluginId != null) {
        val dependencyId = bundledPlugin.pluginId!!
        val pluginDependency = PluginDependencyImpl(dependencyId, true, false)
        val dependencyResult = dependencyFinder.findPluginDependency(pluginDependency)
        val bundledVertex = DepVertex(dependencyId, dependencyResult)
        addTransitiveDependencies(dependenciesGraph, bundledVertex)
      }
    }
  }

}

private data class DepVertex(val pluginId: String, val dependencyResult: DependencyFinder.Result) {

  override fun equals(other: Any?) = other is DepVertex && pluginId == other.pluginId

  override fun hashCode() = pluginId.hashCode()
}

private data class DepEdge(
    val dependency: PluginDependency,
    val sourceVertex: DepVertex,
    val targetVertex: DepVertex
) : DefaultEdge() {
  public override fun getSource() = sourceVertex

  public override fun getTarget() = targetVertex
}

private class DepGraph2ApiGraphConverter(private val ideVersion: IdeVersion) {

  fun convert(graph: DirectedGraph<DepVertex, DepEdge>, startVertex: DepVertex): DependenciesGraph {
    val startNode = graph.toDependencyNode(startVertex)!!
    val vertices = graph.vertexSet().mapNotNull { graph.toDependencyNode(it) }
    val edges = graph.edgeSet().mapNotNull { graph.toDependencyEdge(it) }
    return DependenciesGraph(startNode, vertices, edges)
  }

  private fun DirectedGraph<DepVertex, DepEdge>.toDependencyEdge(depEdge: DepEdge): DependencyEdge? {
    val from = this.toDependencyNode(getEdgeSource(depEdge)) ?: return null
    val to = this.toDependencyNode(getEdgeTarget(depEdge)) ?: return null
    return DependencyEdge(from, to, depEdge.dependency)
  }

  private fun DepEdge.toMissingDependency(): MissingDependency? {
    return with(target.dependencyResult) {
      when (this) {
        is DependencyFinder.Result.DetailsProvided -> {
          with(pluginDetailsCacheResult) {
            when (this) {
              is PluginDetailsCache.Result.Provided -> null
              is PluginDetailsCache.Result.InvalidPlugin -> MissingDependency(
                  dependency,
                  pluginErrors
                      .filter { it.level == PluginProblem.Level.ERROR }
                      .joinToString()
              )
              is PluginDetailsCache.Result.Failed -> MissingDependency(dependency, reason)
              is PluginDetailsCache.Result.FileNotFound -> MissingDependency(dependency, reason)
            }
          }
        }
        is DependencyFinder.Result.NotFound -> MissingDependency(dependency, reason)
        is DependencyFinder.Result.FoundPlugin -> null
      }
    }
  }

  private fun DependencyFinder.Result.getPlugin(): IdePlugin? {
    return when (this) {
      is DependencyFinder.Result.DetailsProvided -> with(pluginDetailsCacheResult) {
        when (this) {
          is PluginDetailsCache.Result.Provided -> pluginDetails.idePlugin
          else -> null
        }
      }
      is DependencyFinder.Result.FoundPlugin -> plugin
      is DependencyFinder.Result.NotFound -> null
    }
  }

  private fun DirectedGraph<DepVertex, DepEdge>.toDependencyNode(depVertex: DepVertex): DependencyNode? {
    val missingDependencies = outgoingEdgesOf(depVertex).mapNotNull { it.toMissingDependency() }
    val plugin = depVertex.dependencyResult.getPlugin()
    return plugin?.run {
      DependencyNode(pluginId ?: depVertex.pluginId, pluginVersion ?: ideVersion.asString(), missingDependencies)
    }
  }

}
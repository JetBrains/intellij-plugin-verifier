/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import org.jgrapht.Graph
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
    val graph = DefaultDirectedGraph<DepVertex, DepEdge>(DepEdge::class.java)
    val missingDependencies = hashMapOf<DepId, MutableSet<DepMissingVertex>>()

    val start = DepVertex(plugin, DependencyFinder.Result.FoundPlugin(plugin))
    addTransitiveDependencies(graph, start, missingDependencies)
    if (plugin.pluginId != CORE_IDE_PLUGIN_ID) {
      maybeAddOptionalJavaPluginDependency(plugin, ide, graph, missingDependencies)
      maybeAddBundledPluginsWithUseIdeaClassLoader(ide, graph, missingDependencies)
    }

    val dependenciesGraph = DepGraph2ApiGraphConverter().convert(graph, start, missingDependencies)
    return dependenciesGraph to graph.vertexSet().map { it.dependencyResult }
  }

  private fun addTransitiveDependencies(
    graph: Graph<DepVertex, DepEdge>,
    vertex: DepVertex,
    missingDependencies: MutableMap<DepId, MutableSet<DepMissingVertex>>
  ) {
    if (!graph.containsVertex(vertex)) {
      graph.addVertex(vertex)

      for (moduleId in vertex.plugin.incompatibleWith) { // TODO migrate: moduleId is pluginId
        val result = dependencyFinder.findPluginDependency(moduleId, true)
        if (result is DependencyFinder.Result.DetailsProvided && result.pluginDetailsCacheResult is PluginDetailsCache.Result.Provided ||
                result is DependencyFinder.Result.FoundPlugin) {
          val depMissingVertex = DepMissingVertex(vertex, PluginDependencyImpl(moduleId, false, true),
                  "The plugin is incompatible with module '$moduleId'")
          missingDependencies.getOrPut(DepId(moduleId, true)) { hashSetOf() } += depMissingVertex
        }
      }

      val dependencies = arrayListOf<PluginDependency>()
      dependencies += vertex.plugin.dependencies
      dependencies += getRecursiveOptionalDependencies(vertex.plugin).map { PluginDependencyImpl(it.id, true, it.isModule) }

      for (pluginDependency in dependencies) {
        val resolvedDependency = resolveDependency(vertex, pluginDependency, graph, missingDependencies) ?: continue

        addTransitiveDependencies(graph, resolvedDependency, missingDependencies)

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
        if (vertex.plugin != resolvedDependency.plugin) {
          graph.addEdge(vertex, resolvedDependency, DepEdge(pluginDependency, vertex, resolvedDependency))
        }
      }
    }
  }

  private fun resolveDependency(
    vertex: DepVertex,
    pluginDependency: PluginDependency,
    graph: Graph<DepVertex, DepEdge>,
    missingDependencies: MutableMap<DepId, MutableSet<DepMissingVertex>>
  ): DepVertex? {
    val depId = DepId(pluginDependency.id, pluginDependency.isModule)

    val existingVertex = graph.vertexSet().find {
      if (depId.isModule) {
        it.plugin.definedModules.contains(depId.id)
      } else {
        it.plugin.pluginId == depId.id
      }
    }
    if (existingVertex != null) {
      return existingVertex
    }

    fun registerMissingDependency(reason: String): DepVertex? {
      missingDependencies.getOrPut(depId) { hashSetOf() } += DepMissingVertex(vertex, pluginDependency, reason)
      return null
    }

    if (depId in missingDependencies) {
      val sameReason = missingDependencies[depId]!!.first().reason
      return registerMissingDependency(sameReason)
    }

    return when (val result = dependencyFinder.findPluginDependency(pluginDependency)) {
      is DependencyFinder.Result.FoundPlugin -> DepVertex(result.plugin, result)
      is DependencyFinder.Result.DetailsProvided -> {
        when (val cacheResult = result.pluginDetailsCacheResult) {
          is PluginDetailsCache.Result.Provided -> DepVertex(cacheResult.pluginDetails.idePlugin, result)
          is PluginDetailsCache.Result.InvalidPlugin -> registerMissingDependency(
            cacheResult.pluginErrors.filter { it.level == PluginProblem.Level.ERROR }.joinToString()
          )
          is PluginDetailsCache.Result.Failed -> registerMissingDependency(cacheResult.reason)
          is PluginDetailsCache.Result.FileNotFound -> registerMissingDependency(cacheResult.reason)
        }
      }
      is DependencyFinder.Result.NotFound -> registerMissingDependency(result.reason)
    }
  }

  private fun getRecursiveOptionalDependencies(plugin: IdePlugin): List<PluginDependency> {
    val allDependencies = arrayListOf<PluginDependency>()
    for (optionalDescriptor in plugin.optionalDescriptors) {
      val optionalPlugin = optionalDescriptor.optionalPlugin
      allDependencies += optionalPlugin.dependencies
      allDependencies += getRecursiveOptionalDependencies(optionalPlugin)
    }
    return allDependencies
  }

  /**
   * If a plugin does not include any module dependency tags in its plugin.xml,
   * it is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA
   * https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html
   *
   * But since we've recently extracted Java to a separate plugin, many plugins may stop working
   * because they depend on Java plugin classes but do not explicitly declare a dependency onto 'com.intellij.modules.java'.
   *
   * So let's forcibly add Java as an optional dependency for such plugins.
   */
  private fun maybeAddOptionalJavaPluginDependency(
    plugin: IdePlugin,
    ide: Ide,
    graph: Graph<DepVertex, DepEdge>,
    missingDependencies: MutableMap<DepId, MutableSet<DepMissingVertex>>
  ) {
    if (ide.findPluginByModule(ALL_MODULES_ID) == null) {
      return
    }
    val isLegacyPlugin = plugin.dependencies.none { it.isModule }
    val isCustomPlugin = ide.bundledPlugins.none { it.pluginId == plugin.pluginId }
    if (isCustomPlugin || isLegacyPlugin) {
      val dependencyResult = dependencyFinder.findPluginDependency(JAVA_MODULE_ID, true)
      val javaPlugin = when (dependencyResult) {
        is DependencyFinder.Result.DetailsProvided -> {
          val providedCacheEntry = dependencyResult.pluginDetailsCacheResult as? PluginDetailsCache.Result.Provided
          providedCacheEntry?.pluginDetails?.idePlugin
        }
        is DependencyFinder.Result.FoundPlugin -> dependencyResult.plugin
        is DependencyFinder.Result.NotFound -> null
      } ?: return
      val javaPluginVertex = DepVertex(javaPlugin, dependencyResult)
      addTransitiveDependencies(graph, javaPluginVertex, missingDependencies)
    }
  }

  /**
   * Bundled plugins that specify `<idea-plugin use-idea-classloader="true">` are automatically added to
   * platform class loader and may be referenced by other plugins without explicit dependency on them.
   *
   * We would like to emulate this behaviour by forcibly adding such plugins to the verification classpath.
   */
  private fun maybeAddBundledPluginsWithUseIdeaClassLoader(
    ide: Ide,
    graph: Graph<DepVertex, DepEdge>,
    missingDependencies: MutableMap<DepId, MutableSet<DepMissingVertex>>
  ) {
    for (bundledPlugin in ide.bundledPlugins) {
      if (bundledPlugin.useIdeClassLoader && bundledPlugin.pluginId != null) {
        val dependencyId = bundledPlugin.pluginId!!
        val pluginDependency = PluginDependencyImpl(dependencyId, true, false)
        val dependencyResult = dependencyFinder.findPluginDependency(pluginDependency.id, pluginDependency.isModule)
        val bundledVertex = DepVertex(bundledPlugin, dependencyResult)
        addTransitiveDependencies(graph, bundledVertex, missingDependencies)
      }
    }
  }

}

private data class DepVertex(val plugin: IdePlugin, val dependencyResult: DependencyFinder.Result) {

  override fun equals(other: Any?) = other is DepVertex && plugin == other.plugin

  override fun hashCode() = plugin.hashCode()
}

private data class DepEdge(
  val dependency: PluginDependency,
  private val sourceVertex: DepVertex,
  private val targetVertex: DepVertex
) : DefaultEdge() {
  public override fun getSource() = sourceVertex

  public override fun getTarget() = targetVertex
}

private data class DepId(val id: String, val isModule: Boolean)

private data class DepMissingVertex(val vertex: DepVertex, val pluginDependency: PluginDependency, val reason: String)

private class DepGraph2ApiGraphConverter {

  fun convert(
    graph: Graph<DepVertex, DepEdge>,
    startVertex: DepVertex,
    vertexMissingDependencies: Map<DepId, Set<DepMissingVertex>>
  ): DependenciesGraph {
    val startNode = startVertex.toDependencyNode()
    val vertices = graph.vertexSet().mapNotNull { it.toDependencyNode() }
    val edges = graph.edgeSet().mapNotNull { edge ->
      val from = graph.getEdgeSource(edge).toDependencyNode()
      val to = graph.getEdgeTarget(edge).toDependencyNode()
      DependencyEdge(from, to, edge.dependency)
    }
    val missingDependencies = hashMapOf<DependencyNode, MutableSet<MissingDependency>>()
    for ((_, missingDeps) in vertexMissingDependencies) {
      for (missingDep in missingDeps) {
        missingDependencies.getOrPut(missingDep.vertex.toDependencyNode()) { hashSetOf() } +=
          MissingDependency(missingDep.pluginDependency, missingDep.reason)
      }
    }
    return DependenciesGraph(startNode, vertices, edges, missingDependencies)
  }

  private fun DepVertex.toDependencyNode(): DependencyNode =
    DependencyNode(plugin.pluginId ?: "<empty id>", plugin.pluginVersion ?: "<empty version>")

}

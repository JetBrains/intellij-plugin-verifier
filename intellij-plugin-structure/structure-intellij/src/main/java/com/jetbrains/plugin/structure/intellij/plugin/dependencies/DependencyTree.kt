/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.intellij.plugin.DependenciesModifier
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PassThruDependenciesModifier
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvider
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency.*
import com.jetbrains.plugin.structure.intellij.plugin.module.IdeModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

private val LOG: Logger = LoggerFactory.getLogger(DependencyTree::class.java)

private const val DEPENDENCY_INDEX_MAX_WIDTH = 3

typealias MissingDependencyListener = (IdePlugin, PluginDependency) -> Unit

private val EMPTY_MISSING_DEPENDENCY_LISTENER: MissingDependencyListener = { _, _ -> }

class DependencyTree(private val pluginProvider: PluginProvider) {

  fun getDependencyTreeResolution(plugin: IdePlugin, dependenciesModifier: DependenciesModifier = PassThruDependenciesModifier): DependencyTreeResolution {
    requireNotNull(plugin.pluginId) { missingId(plugin) }
    val missingDependencies = mutableMapOf<IdePlugin, Set<PluginDependency>>()
    val missingDependencyListener: MissingDependencyListener = { idePlugin: IdePlugin, missingDependency: PluginDependency ->
      missingDependencies.merge(idePlugin, setOf(missingDependency), Set<PluginDependency>::plus)
    }

    val dependencyResolutionContext = ResolutionContext(missingDependencyListener, dependenciesModifier)
    val dependencyGraph = getDependencyGraph(plugin, dependencyResolutionContext)

    val transitiveDependencies = mutableSetOf<Dependency>()
    dependencyGraph.forEachAdjacency { pluginId, dependencies ->
      transitiveDependencies += dependencies
    }

    return DefaultDependencyTreeResolution(plugin, transitiveDependencies, missingDependencies, dependencyGraph)
  }

  @Throws(IllegalArgumentException::class)
  fun getTransitiveDependencies(plugin: IdePlugin): Set<Dependency> {
    return getTransitiveDependencies(plugin, ResolutionContext())
  }

  @Throws(IllegalArgumentException::class)
  fun getTransitiveDependencies(
    plugin: IdePlugin,
    missingDependencyListener: MissingDependencyListener = EMPTY_MISSING_DEPENDENCY_LISTENER,
    dependenciesModifier: DependenciesModifier = PassThruDependenciesModifier
  ): Set<Dependency> {
    return getTransitiveDependencies(plugin, ResolutionContext(missingDependencyListener, dependenciesModifier))
  }

  @Throws(IllegalArgumentException::class)
  private fun getTransitiveDependencies(
    plugin: IdePlugin,
    dependencyResolutionContext: ResolutionContext
  ): Set<Dependency> {
    val pluginId: PluginId = requireNotNull(plugin.pluginId) { missingId(plugin) }
    val graph = getDependencyGraph(plugin, dependencyResolutionContext)
    return graph.collectDependencies(pluginId)
  }

  private fun getDependencyGraph(plugin: IdePlugin, context: ResolutionContext): DiGraph<PluginId, Dependency> {
    val graph = DiGraph<PluginId, Dependency>()
    getDependencyGraph(plugin, graph, resolutionDepth = 0, dependencyIndex = -1, parentDependencyIndex = -1, context)
    return graph
  }

  private fun getDependencyGraph(
    plugin: IdePlugin,
    graph: DiGraph<PluginId, Dependency>,
    resolutionDepth: Int,
    dependencyIndex: Int,
    parentDependencyIndex: Int,
    context: ResolutionContext
  ): Unit =
    with(plugin) {
      val dependencies = context.dependenciesModifier.apply(this, pluginProvider)
      val pluginId = pluginId ?: return@with
      val number = if (dependencyIndex < 0) "" else "" + (dependencyIndex + 1) + ") "
      val indent = getIndent(resolutionDepth, parentDependencyIndex)
      if (dependencies.isEmpty()) {
        debugLog(indent, "${number}No dependencies for '{}'", pluginId)
      } else {
        debugLog(
          indent,
          "${number}Resolving {} ${"dependency".pluralize(dependencies.size)} for '{}': {}",
          dependencies.size,
          pluginId,
          dependencies.joinToString { it.id })

        val nestedIndent = getNestedDependencyIndent(indent, number)
        dependencies.forEachIndexed { i, dep ->
          val dependencyPlugin = pluginProvider.getPluginOrModule(dep.id)
          if (ignore(plugin, dep)) {
            debugLog(nestedIndent, i + 1, "Ignoring '{}'", dep)
          } else if (graph.contains(pluginId, dependencyPlugin)) {
            debugLog(nestedIndent, i + 1, "Resolved cached dependency '{}'", dep.id)
          } else {
            when (dependencyPlugin) {
              is Module,
              is Plugin -> {
                if (dependencyPlugin is PluginAware && !dependencyPlugin.matches(pluginId)) {
                  graph.addEdge(pluginId, dependencyPlugin)
                  getDependencyGraph(dependencyPlugin.plugin, graph, resolutionDepth + 1, i, dependencyIndex, context)
                }
              }
              is None -> {
                context.notifyMissingDependency(plugin, dep)
                debugLog(
                  nestedIndent,
                  numericIndex = i + 1,
                  "Skipping dependency '{}' as it is not available in the IDE.",
                  dep.id
                )
              }
            }
          }
        }
      }
    }

  private fun getNestedDependencyIndent(indent: String, dependencyNumber: String): String {
    val additionalIndent = " ".repeat(max(dependencyNumber.length, DEPENDENCY_INDEX_MAX_WIDTH))
    return indent + additionalIndent
  }

  private fun getIndent(resolutionDepth: Int, parentDependencyIndex: Int): String {
    return if (resolutionDepth <= 1) {
      ""
    } else {
      val suffix =
        if (parentDependencyIndex < 0) "" else " ".repeat(parentDependencyIndex.toString().length - 1) + " ".repeat(
          resolutionDepth - 1
        )
      "  ".repeat(resolutionDepth - 1) + suffix
    }
  }

  private fun ignore(plugin: IdePlugin, dependency: PluginDependency): Boolean {
    return dependency.isModule && plugin.definedModules.contains(dependency.id)
  }

  private fun missingId(plugin: IdePlugin): String {
    val name = plugin.pluginName ?: "unknown name"
    val originalFile = plugin.originalFile ?: "unknown plugin artifact path"
    return "Plugin must have an ID. Name: $name. Path: $originalFile"
  }

  private fun DiGraph<PluginId, Dependency>.collectDependencies(id: PluginId): Set<Dependency> {
    return mutableSetOf<Dependency>().apply {
      collectDependencies(id, this)
    }
  }

  private fun DiGraph<PluginId, Dependency>.collectDependencies(
    id: PluginId,
    dependencies: MutableSet<Dependency>,
    layer: Int = 0
  ) {
    for (dependency in this[id]) {
      if (dependency is PluginAware) {
        val depId = dependency.plugin.pluginId!!
        val dep = if (layer == 0) dependency else dependency.asTransitive()
        if (dep !in dependencies) {
          dependencies += dep
          collectDependencies(depId, dependencies, layer + 1)
        }
      }
    }
  }

  private fun PluginProvider.getPluginOrModule(id: String): Dependency {
    val plugin = this.findPluginById(id)
    return if (plugin != null) {
      if (plugin is IdeModule) {
        Module(plugin, id)
      } else {
        Plugin(plugin)
      }
    } else {
      this.findPluginByModule(id)?.let {
        Module(it, id)
      } ?: None
    }
  }

  private fun DiGraph<PluginId, Dependency>.toDebugString(
    id: PluginId,
    indentSize: Int,
    visited: MutableSet<PluginId>,
    printer: StringBuilder
  ) {
    val indent = "  ".repeat(indentSize)
    this[id].forEach { dep ->
      if (dep is PluginAware) {
        val depId = dep.plugin.pluginId!!
        if (depId !in visited) {
          visited += depId
          printer.appendLine("${indent}* " + dep)
          toDebugString(depId, indentSize + 1, visited, printer)
        } else {
          printer.appendLine("${indent}* $dep (already visited)")
        }
      }
    }
  }

  private fun Dependency.asTransitive(): Dependency {
    return when (this) {
      is Module -> copy(isTransitive = true)
      is Plugin -> copy(isTransitive = true)
      is None -> this
    }
  }

  fun toDebugString(pluginId: String): CharSequence {
    val resolutionContext = ResolutionContext(EMPTY_MISSING_DEPENDENCY_LISTENER)
    return StringBuilder().apply {
      pluginProvider.findPluginById(pluginId)?.let { plugin ->
        getDependencyGraph(plugin, resolutionContext).toDebugString(
          pluginId,
          indentSize = 0,
          mutableSetOf(),
          printer = this
        )
      }
    }
  }

  private fun debugLog(indent: String, message: String, vararg params: Any) {
    debugLog(indent, numericIndex = 0, message, *params)
  }

  private fun debugLog(indent: String, numericIndex: Int, message: String, vararg params: Any) {
    if (LOG.isDebugEnabled) {
      val msg = buildString {
        append(indent)
        if (numericIndex > 0) append(numericIndex).append(") ")
        append(message)
      }
      LOG.debug(msg, *params)
    }
  }

  internal class DiGraph<I, O> {
    private val adjacency = hashMapOf<I, MutableList<O>>()

    operator fun get(from: I): List<O> = adjacency[from] ?: emptyList()

    fun addEdge(from: I, to: O) {
      adjacency.getOrPut(from) { mutableListOf() } += to
    }

    fun contains(from: I, to: O): Boolean = adjacency[from]?.contains(to) == true

    internal fun forEachAdjacency(action: (I, List<O>) -> Unit) {
      adjacency.forEach(action)
    }
  }

  private data class ResolutionContext(
    val missingDependencyListener: MissingDependencyListener = EMPTY_MISSING_DEPENDENCY_LISTENER,
    val dependenciesModifier: DependenciesModifier = PassThruDependenciesModifier
  ) {
    fun notifyMissingDependency(plugin: IdePlugin, dependency: PluginDependency) {
      missingDependencyListener(plugin, dependency)
    }
  }
}
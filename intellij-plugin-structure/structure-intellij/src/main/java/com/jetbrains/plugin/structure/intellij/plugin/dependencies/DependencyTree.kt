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
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision.Source.CONTENT_MODULE_ID
import com.jetbrains.plugin.structure.intellij.plugin.PluginQuery
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.max

private val LOG: Logger = LoggerFactory.getLogger(DependencyTree::class.java)

private const val DEPENDENCY_INDEX_MAX_WIDTH = 3

typealias MissingDependencyListener = (IdePlugin, PluginDependency) -> Unit

private val EMPTY_MISSING_DEPENDENCY_LISTENER: MissingDependencyListener = { _, _ -> }

class DependencyTree(private val pluginProvider: PluginProvider, private val ideModulePredicate: IdeModulePredicate = NegativeIdeModulePredicate) {

  fun getDependencyTreeResolution(
    plugin: IdePlugin,
    dependenciesModifier: DependenciesModifier = PassThruDependenciesModifier
  ): DependencyTreeResolution {
    requireNotNull(plugin.pluginId) { missingId(plugin) }
    val missingDependencies = mutableMapOf<IdePlugin, Set<PluginDependency>>()
    val missingDependencyListener: MissingDependencyListener =
      { idePlugin: IdePlugin, missingDependency: PluginDependency ->
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
      .resolveDuplicateDependencies(dependencyResolutionContext)
  }

  private fun getDependencyGraph(plugin: IdePlugin, context: ResolutionContext): DiGraph<PluginId, Dependency> {
    val graph = DiGraph<PluginId, Dependency>()
    val missingDependencies = MissingDependencies()
    getDependencyGraph(plugin, graph, resolutionDepth = 0, dependencyIndex = -1, parentDependencyIndex = -1,
      missingDependencies, context,
      plugin.id
      )
    return graph
  }

  private fun getDependencyGraph(
    plugin: IdePlugin,
    graph: DiGraph<PluginId, Dependency>,
    resolutionDepth: Int,
    dependencyIndex: Int,
    parentDependencyIndex: Int,
    missingDependencies: MissingDependencies,
    context: ResolutionContext,
    artifactId: PluginId?
  ): Unit =
    with(plugin) {
      val dependencies = context.dependenciesModifier.apply(this, pluginProvider)
      val pluginId = artifactId ?: pluginId ?: return@with
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
          if (ignore(plugin, dep)) {
            debugLog(nestedIndent, i + 1, "Ignoring '{}'", dep)
          } else if (graph.contains(pluginId, hasId(dep))) {
            // TODO log if a dependency might be provided by another plugin with different plugin
            debugLog(nestedIndent, i + 1, "Resolved cached dependency '{}'", dep.id)
          } else if (dep in missingDependencies) {
            debugLog(nestedIndent, i + 1, "Skipping dependency '{}' as it is already marked missing", dep.id)
          } else {
            when (val dependencyPlugin = resolve(dep)) {
              is Module,
              is Plugin -> {
                if (dependencyPlugin is PluginAware && !dependencyPlugin.matches(pluginId)) {
                  graph.addEdge(pluginId, dependencyPlugin)
                  getDependencyGraph(
                    dependencyPlugin.plugin,
                    graph,
                    resolutionDepth + 1,
                    i,
                    dependencyIndex,
                    missingDependencies,
                    context,
                    dependencyPlugin.artifactId
                  )
                }
              }

              is None -> {
                context.notifyMissingDependency(plugin, dep)
                missingDependencies += dep
                debugLog(
                  nestedIndent,
                  numericIndex = i + 1,
                  "Skipping dependency '{}' as it is not available",
                  dep.id
                )
              }
            }
          }
        }
      }
    }

  private fun resolve(dependency: PluginDependency): Dependency {
    return with(dependency) {
      resolvePlugin(id)?.let { provision ->
        val plugin = provision.plugin
        return if (ideModulePredicate.matches(id, plugin)) {
          // It is explicitly declared as a module in the product-info.json in the module list,
          // or it is marked as a module in the product info layout elements.
          Module(plugin, id)
        } else if (provision.source == CONTENT_MODULE_ID) {
          Module(plugin, id)
        } else {
          Plugin(plugin)
        }
      } ?: None
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

  private val Dependency.artifactId: PluginId?
    get() = when (this) {
      is Plugin -> id
      is Module -> id
      None -> null
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
        val dep = if (layer == 0) dependency else dependency.asTransitive()
        if (dep !in dependencies) {
          dependencies += dep
          val dependencyIdAndAliases = listOf(dependency.id) + dependency.plugin.pluginAliases
          for (idOrAlias in dependencyIdAndAliases) {
            collectDependencies(idOrAlias, dependencies, layer + 1)
          }
        }
      }
    }
  }

  private fun resolvePlugin(pluginId: PluginId): PluginProvision.Found? {
    return PluginQuery.Builder.of(pluginId)
      .inId()
      .inName()
      .inPluginAliases()
      .inContentModuleId()
      .build()
      .let {
        pluginProvider.query(it)
      } as? PluginProvision.Found
  }

  private fun Set<Dependency>.resolveDuplicateDependencies(resolutionContext: ResolutionContext): Set<Dependency> {
    if (!resolutionContext.isMergingDuplicateDependencies) return this

    val unique = mutableMapOf<String, Dependency>()
    for (dependency in this) {
      val depId = dependency.artifactId ?: continue
      if (depId in unique) {
        @Suppress("USELESS_IS_CHECK")
        unique[depId] = when (dependency) {
          is Plugin -> dependency.copy(isTransitive = false)
          is Module -> dependency.copy(isTransitive = false)
          is None -> None
        }
      } else {
        unique[depId] = dependency
      }
    }
    return unique.values.toSet()
  }

  private fun DiGraph<PluginId, Dependency>.toDebugString(
    id: PluginId,
    indentSize: Int,
    visited: MutableSet<PluginId>,
    printer: StringBuilder
  ) {
    val indent = "  ".repeat(indentSize)
    this[id]
      .sortedBy { it.id }
      .forEach { dep ->
        if (dep is PluginAware) {
          val depId = dep.id
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

  private fun hasId(dependency: PluginDependency) = { dep: Dependency -> dep.matches(dependency.id) }

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

    fun contains(from: I, toIdPredicate: (O) -> Boolean): Boolean {
      return adjacency[from]?.let { adj ->
        adj.any { toIdPredicate(it) }
      } ?: false
    }

    internal fun forEachAdjacency(action: (I, List<O>) -> Unit) {
      adjacency.forEach(action)
    }
  }

  internal class MissingDependencies {
    private val _missingDependencies = mutableListOf<PluginDependency>()

    operator fun plusAssign(dependency: PluginDependency) {
      _missingDependencies += dependency
    }

    operator fun contains(dependency: PluginDependency): Boolean {
      return dependency in _missingDependencies
    }
  }

  /**
   * A configuration for dependency resolution.
   * @param missingDependencyListener a listener that is invoked when a dependency is missing.
   * @param dependenciesModifier contributes or removes the list of dependencies for a plugin or module
   * @param isMergingDuplicateDependencies indicates whether to merge a single dependency that occurs as a
   * transitive and regular dependency into a single non-transitive dependency
   */
  private data class ResolutionContext(
    val missingDependencyListener: MissingDependencyListener = EMPTY_MISSING_DEPENDENCY_LISTENER,
    val dependenciesModifier: DependenciesModifier = PassThruDependenciesModifier,
    val isMergingDuplicateDependencies: Boolean = true
  ) {
    fun notifyMissingDependency(plugin: IdePlugin, dependency: PluginDependency) {
      missingDependencyListener(plugin, dependency)
    }
  }
}

/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.jetbrains.plugin.structure.base.utils.pluralize
import com.jetbrains.plugin.structure.intellij.plugin.*
import com.jetbrains.plugin.structure.intellij.plugin.PluginProvision.Source.CONTENT_MODULE_ID
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency.*
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency.Module
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import kotlin.math.max

private val LOG: Logger = LoggerFactory.getLogger(DependencyTree::class.java)

private const val DEPENDENCY_INDEX_MAX_WIDTH = 3

typealias MissingDependencyListener = (IdePlugin, PluginDependency) -> Unit

private val EMPTY_MISSING_DEPENDENCY_LISTENER: MissingDependencyListener = { _, _ -> }

class DependencyTree(
  private val pluginProvider: PluginProvider,
  private val ideModulePredicate: IdeModulePredicate = NegativeIdeModulePredicate,
  private val dependencyFilter: (PluginDependency) -> Boolean = { true }
) {

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

    val transitiveDependencies = dependencyGraph.collectDependencies(NodeId.ofPlugin(plugin))
      .resolveDuplicateDependencies(dependencyResolutionContext)

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
    requireNotNull(plugin.pluginId) { missingId(plugin) }
    val graph = getDependencyGraph(plugin, dependencyResolutionContext)
    return graph.collectDependencies(Plugin(plugin).nodeId)
      .resolveDuplicateDependencies(dependencyResolutionContext)
  }

  fun toString(plugin: IdePlugin): CharSequence {
    requireNotNull(plugin.pluginId) { missingId(plugin) }
    val dependenciesModifier = DefaultDependencyContributor(includeContentModuleDependencies = true)
    val graph = getDependencyGraph(plugin, ResolutionContext(dependenciesModifier = dependenciesModifier))
    val visitedNodes = LinkedHashSet<NodeId>()
    val string = StringBuilder()
    graph.toDebugString(NodeId.ofPlugin(plugin), indentSize = 0, visitedNodes, string)
    return string
  }

  private fun getDependencyGraph(plugin: IdePlugin, context: ResolutionContext): DependencyGraph {
    val rootDependency = Plugin(plugin)
    val graph = DependencyGraph(rootDependency)
    val missingDependencies = MissingDependencies()
    getDependencyGraph(
      plugin = plugin,
      nodeId = rootDependency.nodeId,
      graph = graph,
      visitedNodes = LinkedHashSet(),
      resolutionDepth = 0, dependencyIndex = -1, parentDependencyIndex = -1,
      missingDependencies = missingDependencies, context = context, classpathExpandedPlugins = mutableSetOf(),
    )
    return graph
  }

  /**
   * Recursively resolves dependencies of [plugin] and populates [graph] with edges.
   *
   * @param plugin the plugin whose dependencies are being resolved.
   * @param nodeId the graph node identity of [plugin], used as the "from" key when adding edges.
   * @param graph the dependency graph being built.
   * @param visitedNodes tracks already-expanded node identities to prevent infinite recursion on cycles.
   * @param resolutionDepth current recursion depth, used for debug log indentation.
   * @param dependencyIndex index of this dependency in the parent's dependency list, or -1 for the root.
   * @param parentDependencyIndex the [dependencyIndex] of the parent, used for debug log indentation.
   * @param missingDependencies accumulates dependencies that could not be resolved, so they are
   *   skipped on subsequent encounters.
   * @param context resolution configuration (listeners, dependency modifiers, etc.).
   */
  private fun getDependencyGraph(
    plugin: IdePlugin,
    nodeId: NodeId,
    graph: DependencyGraph,
    visitedNodes: MutableSet<NodeId>,
    resolutionDepth: Int,
    dependencyIndex: Int,
    parentDependencyIndex: Int,
    missingDependencies: MissingDependencies,
    context: ResolutionContext,
    classpathExpandedPlugins: MutableSet<IdePlugin>,
    classpathExpansionActive: Boolean = true,
  ): Unit =
    with(plugin) {
      val expandGraph = visitedNodes.add(nodeId)
      val expandClasspath = classpathExpansionActive && classpathExpandedPlugins.add(plugin)
      if (!expandGraph && !expandClasspath) return@with
      val pluginId = pluginId ?: return@with
      // Index content-module descriptors by name for source and sibling dependency lookups below.
      val contentModules = modulesDescriptors.associateBy { it.name }

      addContentModuleOwnershipEdges(plugin, nodeId, contentModules.values, graph)

      val modifications = context.dependenciesModifier.apply(this, pluginProvider)
      val classpathTargets = mutableListOf<Dependency>()
      val indent = getIndent(resolutionDepth, parentDependencyIndex)
      val number = if (dependencyIndex < 0) "" else "${dependencyIndex + 1}) "
      logResolvingDependencies(nodeId, modifications, indent, number)
      val nestedIndent = getNestedDependencyIndent(indent, number)
      modifications.forEachIndexed { i, modification ->
        for (contribution in modification.contributions) {
          val dep = contribution.dependency
          val source = contribution.getSourceNodeId(pluginId, nodeId, contentModules.keys)
          val sibling = contentModules[dep.id]?.takeIf { dep.isModule }
          if (!dependencyFilter(dep)) continue
          if (sibling != null) {
            graph.addEdge(source, Module(plugin, sibling.name).intern())
            continue
          }
          if (ignore(plugin, dep) || dep in missingDependencies) continue
          when (val dependencyPlugin = resolve(dep)) {
            is Plugin, is Module, is ContentModuleDeclaration -> {
              dependencyPlugin as PluginAware
              if (dependencyPlugin.plugin.pluginId == pluginId) continue
              val includeInClasspath = expandClasspath &&
                shouldIncludeInClasspath(dep, dependencyPlugin, classpathTargets)
              if (includeInClasspath) classpathTargets += dependencyPlugin
              graph.addEdge(source, dependencyPlugin, includeInClasspath)
              debugLog(nestedIndent, i + 1, "Resolved '{}' from '{}' (classpath: {})",
                       dep.id, source, includeInClasspath)
              getDependencyGraph(
                dependencyPlugin.plugin, dependencyPlugin.nodeId!!, graph, visitedNodes,
                resolutionDepth + 1, i, dependencyIndex, missingDependencies, context, classpathExpandedPlugins,
                classpathExpansionActive = includeInClasspath
              )
            }

            is None -> {
              context.notifyMissingDependency(plugin, dep)
              missingDependencies += dep
              debugLog(nestedIndent, i + 1, "Skipping dependency '{}' as it is not available", dep.id)
            }
          }
        }
      }
    }

  private fun addContentModuleOwnershipEdges(
    plugin: IdePlugin,
    nodeId: NodeId,
    contentModules: Collection<ModuleDescriptor>,
    graph: DependencyGraph
  ) {
    // <content><module> establishes ownership without an explicit <dependencies> entry. Avoid self-edges.
    for (descriptor in contentModules) {
      val moduleDependency = ContentModuleDeclaration(plugin, descriptor.name).intern()
      if (moduleDependency.nodeId != nodeId) {
        graph.addOwnershipEdge(nodeId, moduleDependency)
      }
    }
  }

  private fun DependencyContribution.getSourceNodeId(
    pluginId: PluginId,
    nodeId: NodeId,
    contentModuleNames: Set<String>
  ): NodeId = when (this) {
    is ContentModuleDependencyContribution -> contributingContentModule
      .takeIf { it in contentModuleNames }
      ?.let { NodeId(pluginId, it) } ?: nodeId
    is PluginMainModuleDependencyContribution -> nodeId
  }

  private fun shouldIncludeInClasspath(
    dependency: PluginDependency,
    resolvedDependency: Dependency,
    classpathTargets: List<Dependency>
  ): Boolean = classpathTargets.none {
    val contentModule = it is Module &&
      it.plugin.modulesDescriptors.any { descriptor -> descriptor.name == it.id } &&
      !ideModulePredicate.matches(it.id, it.plugin)
    it.matches(dependency.id) && !(contentModule && resolvedDependency is Plugin)
  }

  private fun logResolvingDependencies(
    nodeId: NodeId,
    modifications: List<DependencyModification>,
    indent: String,
    number: String
  ) {
    debugLog(indent, "${number}Resolving {} ${"dependency".pluralize(modifications.size)} for '{}': {}",
      modifications.size, nodeId, modifications.joinToString { it.dependency.id })
  }

  // It's OK to keep them all in memory since they're held in memory by:
  // DiGraph
  //  <- DefaultDependencyTreeResolution
  //   <- DependencyTreeAwareResolver
  //    <- CachingPluginDependencyResolverProvider.cache
  //     <- DefaultClassResolverProvider.pluginResolverProvider
  //      <- PluginVerificationDescriptor$IDE.classResolverProvider
  //       <- PluginVerifier.verificationDescriptor
  private val dependencyCache: ConcurrentHashMap<Dependency, Dependency> = ConcurrentHashMap()

  private fun Dependency.intern(): Dependency {
    return dependencyCache.computeIfAbsent(this, Function.identity())
  }

  private fun resolve(dependency: PluginDependency): Dependency {
    val id = dependency.id
    val found = resolvePlugin(id) ?: return None
    val plugin = found.plugin
    val dep = if (ideModulePredicate.matches(id, plugin)) {
      // It is explicitly declared as a module in the product-info.json in the module list,
      // or it is marked as a module in the product info layout elements.
      Module(plugin, id)
    } else if (found.source == CONTENT_MODULE_ID) {
      Module(plugin, id)
    } else {
      Plugin(plugin)
    }
    return dep.intern()
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
    return !dependencyFilter(dependency) ||
      (dependency.isModule && plugin.hasDefinedModuleWithId(dependency.id))
  }

  private val Dependency.artifactId: PluginId?
    get() = when (this) {
      is Plugin -> id
      is Module -> id
      is ContentModuleDeclaration -> id
      None -> null
    }

  private fun DependencyGraph.collectDependencies(nodeId: NodeId): Set<Dependency> {
    val dependencies = linkedSetOf<Dependency>()
    val pending = ArrayDeque<Pair<NodeId, Int>>()
    val visited = mutableSetOf<Pair<NodeId, Int>>()
    pending.add(nodeId to 0)
    while (pending.isNotEmpty()) {
      val (from, layer) = pending.removeFirst()
      if (!visited.add(from to layer)) continue
      for (dependency in getClasspathDependencies(from)) {
        val target = dependency.nodeId ?: continue
        val internal = isOwnershipEdge(from, target) || from.pluginId == target.pluginId
        if (!internal) dependencies += (if (layer == 0) dependency else dependency.asTransitive()).intern()
        pending.add(target to if (internal) layer else (layer + 1).coerceAtMost(2))
      }
    }
    return dependencies
  }

  private val pluginCache: Cache<PluginId, PluginProvision.Found> = Caffeine.newBuilder()
    .softValues()
    .build()

  private fun resolvePlugin(pluginId: PluginId): PluginProvision.Found? {
    val id = pluginId.intern()
    val result = pluginCache.getIfPresent(id)
    if (result != null) return result
    // it's OK to synchronize on PluginId (String) since we've interned it.
    synchronized(id) {
      val result = pluginCache.getIfPresent(id)
      if (result != null) return result
      val resolved = doResolvePlugin(id)
      if (resolved != null) {
        pluginCache.put(id, resolved)
      }
      return resolved
    }
  }

  private fun doResolvePlugin(pluginId: PluginId): PluginProvision.Found? {
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
          is ContentModuleDeclaration -> dependency
          is None -> None
        }.intern()
      } else {
        unique[depId] = dependency
      }
    }
    return unique.values.toSet()
  }

  private fun DependencyGraph.toDebugString(
    nodeId: NodeId,
    indentSize: Int,
    visited: MutableSet<NodeId>,
    printer: StringBuilder
  ) {
    val indent = "  ".repeat(indentSize)
    this[nodeId]
      .sortedBy { it.artifactId }
      .forEach { dep ->
        val depNodeId = dep.nodeId
        if (depNodeId != null) {
          if (depNodeId !in visited) {
            visited += depNodeId
            printer.appendLine("${indent}* " + dep)
            toDebugString(depNodeId, indentSize + 1, visited, printer)
          } else {
            printer.appendLine("${indent}* $dep (already visited)")
          }
        }
    }
  }

  private fun Dependency.asTransitive(): Dependency {
    return when (this) {
      is ContentModuleDeclaration -> this
      is Module -> copy(isTransitive = true)
      is Plugin -> copy(isTransitive = true)
      is None -> this
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

  /**
   * A directed graph of plugin and module dependencies, keyed by [NodeId].
   *
   * Using [NodeId] rather than plain plugin IDs ensures that distinct modules
   * provided by the same plugin are represented as separate nodes in the graph.
   */
  internal class DependencyGraph {
    private val nodeIndex = hashMapOf<NodeId, Dependency>()
    private val adjacency = linkedMapOf<NodeId, MutableList<Dependency>>()
    private val ownershipEdges = mutableSetOf<Pair<NodeId, NodeId>>()
    private val classpathEdges = mutableSetOf<Pair<NodeId, NodeId>>()

    constructor(rootPlugin: Dependency) {
      val nodeId = requireNotNull(rootPlugin.nodeId) { "Root plugin must be a Plugin or Module" }
      nodeIndex[nodeId] = rootPlugin
    }

    operator fun get(from: NodeId): List<Dependency> = adjacency[from] ?: emptyList()

    fun getClasspathDependencies(from: NodeId): List<Dependency> = this[from].filter {
      from to it.nodeId in classpathEdges
    }

    fun addOwnershipEdge(from: NodeId, to: Dependency) {
      ownershipEdges += from to requireNotNull(to.nodeId)
      addEdge(from, to)
    }

    fun isOwnershipEdge(from: NodeId, to: NodeId): Boolean = from to to in ownershipEdges

    fun addEdge(from: NodeId, to: Dependency, includeInClasspath: Boolean = true) {
      val toNodeId = to.nodeId
      if (toNodeId != null) {
        nodeIndex.putIfAbsent(toNodeId, to)
        if (includeInClasspath) classpathEdges += from to toNodeId
      }
      val targets = adjacency.getOrPut(from) { mutableListOf() }
      if (targets.none { it.nodeId == toNodeId }) targets += to
    }

    fun contains(from: NodeId, toIdPredicate: (Dependency) -> Boolean): Boolean {
      return adjacency[from]?.any(toIdPredicate) ?: false
    }

    internal fun forEachAdjacency(action: (Dependency, List<Dependency>) -> Unit) {
      adjacency.forEach { (from, to) ->
        val fromNode = nodeIndex[from]
        if (fromNode == null) {
          LOG.warn("Node not found for $from")
          return@forEach
        }
        action(fromNode, to)
      }
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

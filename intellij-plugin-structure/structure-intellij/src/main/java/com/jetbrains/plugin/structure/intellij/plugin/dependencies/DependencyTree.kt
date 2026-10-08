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
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function
import kotlin.collections.ArrayDeque
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

    return DefaultDependencyTreeResolution(plugin, transitiveDependencies, missingDependencies, dependencyGraph.compact())
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
    val graph = getDependencyGraph(plugin, ResolutionContext())
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
      missingDependencies = missingDependencies, context = context, classpathExpandedNodes = mutableSetOf(),
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
    classpathExpandedNodes: MutableSet<NodeId>,
    classpathExpansionActive: Boolean = true,
  ): Unit =
    with(plugin) {
      val expandGraph = visitedNodes.add(nodeId)
      val expandClasspath = classpathExpansionActive && classpathExpandedNodes.add(nodeId)
      if (!expandGraph && !expandClasspath) return@with
      val pluginId = pluginId ?: return@with
      // Modifications and content modules are computed once per plugin and resolution.
      val pluginDependencyContext = context.getDependencyContext(plugin)
      val contentModules = pluginDependencyContext.contentModules
      // A content module node resolves only its own dependencies.
      val currentContentModule = nodeId.moduleId?.takeIf { it in contentModules }

      if (isAliasNode(nodeId, pluginId, currentContentModule)) {
        // An alias (e.g. 'com.intellij.modules.platform') is provided by the plugin main node.
        // Redirect to the main node, so that the plugin is expanded only once, regardless of the number of aliases.
        val mainDependency = Plugin(plugin).intern()
        graph.addAliasEdge(nodeId, mainDependency, includeInClasspath = expandClasspath)
        getDependencyGraph(
          plugin, mainDependency.nodeId!!, graph, visitedNodes,
          resolutionDepth, dependencyIndex, parentDependencyIndex, missingDependencies, context, classpathExpandedNodes,
          classpathExpansionActive = expandClasspath
        )
        return@with
      }

      if (currentContentModule == null) {
        addContentModuleOwnershipEdges(plugin, nodeId, contentModules.values, graph)
      }

      val modifications = pluginDependencyContext.modifications
      val classpathTargets = mutableListOf<Dependency>()
      val indent = getIndent(resolutionDepth, parentDependencyIndex)
      val number = if (dependencyIndex < 0) "" else "${dependencyIndex + 1}) "
      logResolvingDependencies(nodeId, modifications, indent, number)
      val nestedIndent = getNestedDependencyIndent(indent, number)
      // The main node resolves contributions of the main module and all content modules.
      // A content module node resolves only the contributions of that content module.
      val contributions = if (currentContentModule == null) {
        pluginDependencyContext.allContributions
      } else {
        pluginDependencyContext.getContentModuleContributions(currentContentModule)
      }
      for ((i, contribution) in contributions) {
        val dep = contribution.dependency
        val source = contribution.getSourceNodeId(pluginId, nodeId, contentModules.keys)
        val sibling = contentModules[dep.id]?.takeIf { dep.isModule }
        if (!dependencyFilter(dep)) continue
        if (sibling != null) {
          val siblingDependency = Module(plugin, sibling.name).intern()
          graph.addEdge(source, siblingDependency)
          if (currentContentModule != null) {
            getDependencyGraph(
              plugin, siblingDependency.nodeId!!, graph, visitedNodes,
              resolutionDepth + 1, i, dependencyIndex, missingDependencies, context, classpathExpandedNodes,
              classpathExpansionActive = expandClasspath
            )
          }
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
              resolutionDepth + 1, i, dependencyIndex, missingDependencies, context, classpathExpandedNodes,
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

  /**
   * A module node that is neither a content module of its plugin nor the plugin itself
   * (as is the case for IDE modules whose module ID equals the plugin ID) represents an alias of the plugin.
   */
  private fun isAliasNode(nodeId: NodeId, pluginId: PluginId, currentContentModule: String?): Boolean {
    val moduleId = nodeId.moduleId ?: return false
    return currentContentModule == null && moduleId != pluginId
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
    // A content module covers only itself, not its owning plugin or sibling content modules.
    if (contentModule) {
      it.id == dependency.id && resolvedDependency !is Plugin
    } else {
      it.matches(dependency.id)
    }
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
    // An alias node shows the dependencies of its plugin main node directly, without the internal alias edge.
    val aliasTarget = getAliasTarget(nodeId)
    val dependencies = if (aliasTarget != null) this[nodeId] + this[aliasTarget] else this[nodeId]
    dependencies
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

    /**
     * Outgoing edges per source node, keyed by target node identity. Each edge is stored exactly once,
     * with its classpath and ownership flags. Insertion order of targets is preserved.
     */
    private val adjacency = linkedMapOf<NodeId, LinkedHashMap<NodeId?, Edge>>()

    constructor(rootPlugin: Dependency) {
      val nodeId = requireNotNull(rootPlugin.nodeId) { "Root plugin must be a Plugin or Module" }
      nodeIndex[nodeId] = rootPlugin
    }

    /**
     * Returns the targets of the visible edges of [from]. Internal alias edges are not included.
     */
    operator fun get(from: NodeId): List<Dependency> = adjacency[from]?.values?.visibleTargets() ?: emptyList()

    /**
     * Returns the classpath targets of [from], including the targets of internal alias edges.
     */
    fun getClasspathDependencies(from: NodeId): List<Dependency> {
      val edges = adjacency[from] ?: return emptyList()
      return edges.values.mapNotNull { edge -> edge.target.takeIf { edge.classpath } }
    }

    /**
     * Adds an ownership edge. An ownership edge is always a classpath edge.
     */
    fun addOwnershipEdge(from: NodeId, to: Dependency) {
      requireNotNull(to.nodeId)
      addEdge(from, to, includeInClasspath = true, ownership = true)
    }

    fun isOwnershipEdge(from: NodeId, to: NodeId): Boolean = adjacency[from]?.get(to)?.ownership ?: false

    /**
     * Adds an edge from [from] to [to]. Adding an already existing edge keeps the originally added target
     * and merges the classpath and ownership flags.
     */
    fun addEdge(from: NodeId, to: Dependency, includeInClasspath: Boolean = true) {
      addEdge(from, to, includeInClasspath, ownership = false)
    }

    /**
     * Adds an internal link from an alias node to its plugin main node.
     * The link is used for resolution only and is hidden from all views of this graph.
     */
    fun addAliasEdge(from: NodeId, to: Dependency, includeInClasspath: Boolean) {
      addEdge(from, to, includeInClasspath, ownership = false, alias = true)
    }

    /**
     * Returns the target of the internal alias edge of [from], if any.
     */
    fun getAliasTarget(from: NodeId): NodeId? = adjacency[from]?.values?.firstOrNull { it.alias }?.target?.nodeId

    private fun addEdge(from: NodeId, to: Dependency, includeInClasspath: Boolean, ownership: Boolean, alias: Boolean = false) {
      val toNodeId = to.nodeId
      if (toNodeId != null) {
        nodeIndex.putIfAbsent(toNodeId, to)
      }
      val edges = adjacency.getOrPut(from) { LinkedHashMap() }
      val edge = edges[toNodeId]
      if (edge == null) {
        edges[toNodeId] = Edge(to, classpath = includeInClasspath && toNodeId != null, ownership = ownership, alias = alias)
      } else {
        if (includeInClasspath && toNodeId != null) edge.classpath = true
        if (ownership) edge.ownership = true
        // A regular edge to the same target makes the edge visible.
        if (!alias) edge.alias = false
      }
    }

    fun contains(from: NodeId, toIdPredicate: (Dependency) -> Boolean): Boolean {
      return adjacency[from]?.values?.any { !it.alias && toIdPredicate(it.target) } ?: false
    }

    internal fun forEachAdjacency(action: (Dependency, List<Dependency>) -> Unit) {
      adjacency.forEach { (from, edges) ->
        val fromNode = nodeIndex[from]
        if (fromNode == null) {
          LOG.warn("Node not found for $from")
          return@forEach
        }
        val targets = edges.values.visibleTargets()
        if (targets.isNotEmpty()) {
          action(fromNode, targets)
        }
      }
    }

    private fun Collection<Edge>.visibleTargets(): List<Dependency> = mapNotNull { edge -> edge.target.takeUnless { edge.alias } }

    /**
     * Creates a compact, read-only copy of the adjacency of this graph.
     * It does not retain the node index and edge flags that are needed only while the graph is being built.
     */
    internal fun compact(): CompactDependencyGraph {
      val sources = ArrayList<Dependency>(adjacency.size)
      val targets = ArrayList<Array<Dependency>>(adjacency.size)
      forEachAdjacency { from, dependencies ->
        sources += from
        targets += dependencies.toTypedArray()
      }
      return CompactDependencyGraph(sources.toTypedArray(), targets.toTypedArray())
    }

    private class Edge(val target: Dependency, var classpath: Boolean, var ownership: Boolean, var alias: Boolean)
  }

  /**
   * A read-only adjacency of a [DependencyGraph], retained by [DefaultDependencyTreeResolution].
   * The adjacency of the source at index `i` in [sources] is stored in [targets] at the same index.
   */
  internal class CompactDependencyGraph(
    private val sources: Array<Dependency>,
    private val targets: Array<Array<Dependency>>
  ) {
    fun forEachAdjacency(action: (Dependency, List<Dependency>) -> Unit) {
      for (i in sources.indices) {
        action(sources[i], targets[i].asList())
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
    /**
     * Per-resolution memo of plugin dependency contexts. Keyed by identity, as [IdePlugin] equality is not reliable.
     */
    val pluginDependencyContexts = IdentityHashMap<IdePlugin, PluginDependencyContext>()

    fun notifyMissingDependency(plugin: IdePlugin, dependency: PluginDependency) {
      missingDependencyListener(plugin, dependency)
    }
  }

  /**
   * Dependency modifications and content modules of a single plugin, computed once per resolution
   * and shared by the plugin main node and all its content module nodes.
   *
   * @param modifications the result of the [DependenciesModifier] applied to the plugin.
   * @param contentModules content-module descriptors indexed by name.
   * @param allContributions all contributions, each paired with the index of its modification.
   * @param contentModuleContributions contributions declared by each content module, paired with the index of
   * their modification.
   */
  private class PluginDependencyContext(
    val modifications: List<DependencyModification>,
    val contentModules: Map<String, ModuleDescriptor>,
    val allContributions: List<IndexedValue<DependencyContribution>>,
    private val contentModuleContributions: Map<String, List<IndexedValue<DependencyContribution>>>
  ) {
    fun getContentModuleContributions(contentModule: String): List<IndexedValue<DependencyContribution>> =
      contentModuleContributions[contentModule].orEmpty()
  }

  private fun ResolutionContext.getDependencyContext(plugin: IdePlugin): PluginDependencyContext = pluginDependencyContexts.getOrPut(plugin) {
    val modifications = dependenciesModifier.apply(plugin, pluginProvider)
    val allContributions = mutableListOf<IndexedValue<DependencyContribution>>()
    val contentModuleContributions = hashMapOf<String, MutableList<IndexedValue<DependencyContribution>>>()
    modifications.forEachIndexed { i, modification ->
      for (contribution in modification.contributions) {
        val indexedContribution = IndexedValue(i, contribution)
        allContributions += indexedContribution
        if (contribution is ContentModuleDependencyContribution) {
          contentModuleContributions.getOrPut(contribution.contributingContentModule) { mutableListOf() } += indexedContribution
        }
      }
    }
    PluginDependencyContext(modifications, plugin.modulesDescriptors.associateBy { it.name }, allContributions, contentModuleContributions)
  }
}

/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class DependencyTreeDependencyGraphTest {
  @Test
  fun `graph is generated correctly`() {
    val somePluginId = "com.example.SomePlugin"
    val alphaPluginId = "com.example.Alpha"
    val betaPluginId = "com.example.Beta"
    val comIntellijPluginId = "com.intellij"

    val somePlugin = mockk<IdePlugin>()
    every { somePlugin.pluginId } returns somePluginId

    val alphaPlugin = mockk<IdePlugin>()
    every { alphaPlugin.pluginId } returns alphaPluginId

    val betaPlugin = mockk<IdePlugin>()
    every { betaPlugin.pluginId } returns betaPluginId

    val comIntellijPlugin = mockk<IdePlugin>()
    every { comIntellijPlugin.pluginId } returns comIntellijPluginId

    val graph = DependencyTree.DependencyGraph(Dependency.Plugin(somePlugin))
    graph.addEdge(NodeId(somePluginId, null), Dependency.Plugin(alphaPlugin))
    graph.addEdge(NodeId(somePluginId, null), Dependency.Plugin(betaPlugin))
    graph.addEdge(NodeId(alphaPluginId, null), Dependency.Plugin(comIntellijPlugin))
    graph.addEdge(NodeId(betaPluginId, null), Dependency.Plugin(comIntellijPlugin))

    val vertices = mutableSetOf<PluginId>()
    graph.forEachAdjacency { from, dependencies ->
      vertices.add(from.id)
      dependencies.forEach {
        vertices += it.id
      }
    }
    assertEquals(setOf(somePluginId, alphaPluginId, betaPluginId, comIntellijPluginId), vertices)

    val somePluginDependsOnAlpha = graph.contains(NodeId(somePluginId, null)) {
      it.matches(alphaPluginId)
    }
    assertTrue(somePluginDependsOnAlpha)
  }

  @Test
  fun `ownership declarations and resolved sibling references share node identities`() {
    val plugin = mockk<IdePlugin>()
    every { plugin.pluginId } returns "owner"
    val externalPlugin = mockk<IdePlugin>()
    every { externalPlugin.pluginId } returns "external"
    val root = Dependency.Plugin(plugin)
    val main = Dependency.ContentModuleDeclaration(plugin, "main")
    val extra = Dependency.ContentModuleDeclaration(plugin, "extra")
    val mainReference = Dependency.Module(plugin, "main")
    val external = Dependency.Plugin(externalPlugin)
    val graph = DependencyTree.DependencyGraph(root)

    graph.addOwnershipEdge(root.nodeId, main)
    graph.addOwnershipEdge(root.nodeId, extra)
    graph.addEdge(root.nodeId, mainReference)
    graph.addEdge(extra.nodeId, mainReference)
    graph.addEdge(main.nodeId, external)

    assertEquals(listOf(main, extra), graph[root.nodeId])
    assertEquals(listOf(mainReference), graph[extra.nodeId])
    assertEquals(listOf(external), graph[main.nodeId])
    assertTrue(graph.isOwnershipEdge(root.nodeId, main.nodeId))
    assertTrue(graph.isOwnershipEdge(root.nodeId, extra.nodeId))
    assertFalse(graph.isOwnershipEdge(extra.nodeId, main.nodeId))
    assertTrue(graph.contains(root.nodeId) { it.matches("main") })

    val edges = mutableListOf<Pair<Dependency, Dependency>>()
    val resolution = DefaultDependencyTreeResolution(plugin, emptySet(), emptyMap(), graph.compact())
    resolution.forEach { from, to -> edges += from to to }
    assertEquals(listOf(root to main, root to extra, extra to mainReference, main to external), edges)
  }

  @Test
  fun `graph iteration preserves plugin and module node identities for a shared owner`() {
    val rootPlugin = mockk<IdePlugin>()
    every { rootPlugin.pluginId } returns "com.example.Root"
    val ownerPlugin = mockk<IdePlugin>()
    every { ownerPlugin.pluginId } returns "com.example.Owner"
    val externalPlugin = mockk<IdePlugin>()
    every { externalPlugin.pluginId } returns "com.example.External"

    val root = Dependency.Plugin(rootPlugin)
    val owner = Dependency.Plugin(ownerPlugin)
    val core = Dependency.Module(ownerPlugin, "com.example.Owner.core")
    val extras = Dependency.Module(ownerPlugin, "com.example.Owner.extras")
    val external = Dependency.Plugin(externalPlugin)
    val graph = DependencyTree.DependencyGraph(root)
    graph.addEdge(root.nodeId, owner)
    graph.addEdge(root.nodeId, core)
    graph.addEdge(root.nodeId, extras)
    graph.addEdge(owner.nodeId, core)
    graph.addEdge(owner.nodeId, extras)
    graph.addEdge(owner.nodeId, external)
    graph.addEdge(core.nodeId, extras)
    graph.addEdge(core.nodeId, external)
    graph.addEdge(extras.nodeId, core)
    graph.addEdge(extras.nodeId, external)

    val adjacency = linkedMapOf<Dependency, List<Dependency>>()
    graph.forEachAdjacency { from, dependencies ->
      adjacency[from] = dependencies
    }
    assertEquals(mapOf(
      root to listOf(owner, core, extras),
      owner to listOf(core, extras, external),
      core to listOf(extras, external),
      extras to listOf(core, external)
    ), adjacency)
    assertSame(owner, adjacency.keys.single { it.nodeId == owner.nodeId })
    assertSame(core, adjacency.keys.single { it.nodeId == core.nodeId })
    assertSame(extras, adjacency.keys.single { it.nodeId == extras.nodeId })
    assertEquals(listOf(core, extras, external), graph[owner.nodeId])
    assertEquals(listOf(extras, external), graph[core.nodeId])
    assertEquals(listOf(core, external), graph[extras.nodeId])
    assertTrue(graph.contains(core.nodeId) { it.nodeId == extras.nodeId })
    assertFalse(graph.contains(core.nodeId) { it.nodeId == owner.nodeId })

    val resolution = DefaultDependencyTreeResolution(rootPlugin, emptySet(), emptyMap(), graph.compact())
    val iteratedEdges = mutableListOf<Pair<Dependency, Dependency>>()
    resolution.forEach { from, to -> iteratedEdges += from to to }
    assertEquals(adjacency.flatMap { (from, dependencies) -> dependencies.map { from to it } }, iteratedEdges)
    assertEquals(setOf(root.nodeId, owner.nodeId, core.nodeId, extras.nodeId), iteratedEdges.map { it.first.nodeId }.toSet())
    assertEquals(setOf(owner.nodeId, core.nodeId, extras.nodeId, external.nodeId), iteratedEdges.map { it.second.nodeId }.toSet())
  }

  @Test
  fun `duplicate edges are stored once and flags are merged`() {
    val rootPlugin = mockk<IdePlugin>()
    every { rootPlugin.pluginId } returns "com.example.Root"
    val alphaPlugin = mockk<IdePlugin>()
    every { alphaPlugin.pluginId } returns "com.example.Alpha"
    val betaPlugin = mockk<IdePlugin>()
    every { betaPlugin.pluginId } returns "com.example.Beta"

    val root = Dependency.Plugin(rootPlugin)
    val alpha = Dependency.Plugin(alphaPlugin)
    val beta = Dependency.Plugin(betaPlugin)
    val graph = DependencyTree.DependencyGraph(root)

    graph.addEdge(root.nodeId, alpha, includeInClasspath = false)
    graph.addEdge(root.nodeId, beta)
    graph.addEdge(root.nodeId, alpha.copy(isTransitive = true))
    assertEquals(listOf(alpha, beta), graph[root.nodeId])
    assertSame(alpha, graph[root.nodeId].first())
    assertEquals(listOf(alpha, beta), graph.getClasspathDependencies(root.nodeId))

    graph.addEdge(root.nodeId, beta, includeInClasspath = false)
    assertEquals(listOf(alpha, beta), graph.getClasspathDependencies(root.nodeId))
  }

  @Test
  fun `non-classpath edge is not a classpath dependency`() {
    val rootPlugin = mockk<IdePlugin>()
    every { rootPlugin.pluginId } returns "com.example.Root"
    val alphaPlugin = mockk<IdePlugin>()
    every { alphaPlugin.pluginId } returns "com.example.Alpha"

    val root = Dependency.Plugin(rootPlugin)
    val alpha = Dependency.Plugin(alphaPlugin)
    val graph = DependencyTree.DependencyGraph(root)

    graph.addEdge(root.nodeId, alpha, includeInClasspath = false)
    assertEquals(listOf(alpha), graph[root.nodeId])
    assertTrue(graph.getClasspathDependencies(root.nodeId).isEmpty())
  }

  @Test
  fun `ownership edge implies classpath edge`() {
    val plugin = mockk<IdePlugin>()
    every { plugin.pluginId } returns "owner"
    val root = Dependency.Plugin(plugin)
    val module = Dependency.ContentModuleDeclaration(plugin, "owner.module")
    val graph = DependencyTree.DependencyGraph(root)

    graph.addEdge(root.nodeId, Dependency.Module(plugin, "owner.module"), includeInClasspath = false)
    assertFalse(graph.isOwnershipEdge(root.nodeId, module.nodeId))
    assertTrue(graph.getClasspathDependencies(root.nodeId).isEmpty())

    graph.addOwnershipEdge(root.nodeId, module)
    assertTrue(graph.isOwnershipEdge(root.nodeId, module.nodeId))
    assertEquals(1, graph[root.nodeId].size)
    assertEquals(1, graph.getClasspathDependencies(root.nodeId).size)
    assertFalse(graph.isOwnershipEdge(module.nodeId, root.nodeId))
  }

  @Test
  fun `alias edge is used for resolution only and hidden from all views`() {
    val rootPlugin = mockk<IdePlugin>()
    every { rootPlugin.pluginId } returns "com.example.Root"
    val platformPlugin = mockk<IdePlugin>()
    every { platformPlugin.pluginId } returns "com.intellij"
    val externalPlugin = mockk<IdePlugin>()
    every { externalPlugin.pluginId } returns "com.example.External"

    val root = Dependency.Plugin(rootPlugin)
    val alias = Dependency.Module(platformPlugin, "com.intellij.modules.platform")
    val main = Dependency.Plugin(platformPlugin)
    val external = Dependency.Plugin(externalPlugin)
    val graph = DependencyTree.DependencyGraph(root)
    graph.addEdge(root.nodeId, alias)
    graph.addAliasEdge(alias.nodeId, main, includeInClasspath = true)
    graph.addEdge(main.nodeId, external)

    assertTrue(graph[alias.nodeId].isEmpty())
    assertFalse(graph.contains(alias.nodeId) { it.nodeId == main.nodeId })
    assertEquals(listOf(main), graph.getClasspathDependencies(alias.nodeId))
    assertEquals(main.nodeId, graph.getAliasTarget(alias.nodeId))
    assertNull(graph.getAliasTarget(root.nodeId))

    val adjacency = linkedMapOf<Dependency, List<Dependency>>()
    graph.forEachAdjacency { from, dependencies -> adjacency[from] = dependencies }
    assertEquals(mapOf(root to listOf(alias), main to listOf(external)), adjacency)

    val edges = mutableListOf<Pair<Dependency, Dependency>>()
    DefaultDependencyTreeResolution(rootPlugin, emptySet(), emptyMap(), graph.compact())
      .forEach { from, to -> edges += from to to }
    assertEquals(listOf(root to alias, main to external), edges)
  }

  @Test
  fun `regular edge to an alias target makes the edge visible`() {
    val plugin = mockk<IdePlugin>()
    every { plugin.pluginId } returns "com.intellij"
    val alias = Dependency.Module(plugin, "com.intellij.modules.platform")
    val main = Dependency.Plugin(plugin)
    val graph = DependencyTree.DependencyGraph(alias)

    graph.addAliasEdge(alias.nodeId, main, includeInClasspath = false)
    assertTrue(graph[alias.nodeId].isEmpty())
    assertTrue(graph.getClasspathDependencies(alias.nodeId).isEmpty())

    graph.addEdge(alias.nodeId, main)
    assertEquals(listOf(main), graph[alias.nodeId])
    assertEquals(listOf(main), graph.getClasspathDependencies(alias.nodeId))
    assertNull(graph.getAliasTarget(alias.nodeId))

    graph.addAliasEdge(alias.nodeId, main, includeInClasspath = true)
    assertEquals(listOf(main), graph[alias.nodeId])
  }

  @Test
  fun `node identity is computed once and does not affect equality`() {
    val plugin = mockk<IdePlugin>()
    every { plugin.pluginId } returns "owner"
    val pluginDependency = Dependency.Plugin(plugin)
    val moduleDependency = Dependency.Module(plugin, "owner.module")
    val declaration = Dependency.ContentModuleDeclaration(plugin, "owner.module")

    assertSame(pluginDependency.nodeId, pluginDependency.nodeId)
    assertSame(moduleDependency.nodeId, moduleDependency.nodeId)
    assertSame(declaration.nodeId, declaration.nodeId)
    assertEquals(moduleDependency.nodeId, declaration.nodeId)
    assertEquals(Dependency.Plugin(plugin), pluginDependency)
    assertEquals(Dependency.Plugin(plugin).hashCode(), pluginDependency.hashCode())
  }

  // FIXME Duplicate from com.jetbrains.plugin.structure.ide.classes.resolver.CachingPluginDependencyResolverProvider.getPluginId
  private val Dependency.id: String
    get() {
      return when (this) {
        is Dependency.ContentModuleDeclaration -> this.plugin.pluginId ?: this.plugin.pluginName
        is Dependency.Module -> this.plugin.pluginId ?: this.plugin.pluginName
        is Dependency.Plugin -> this.plugin.pluginId ?: this.plugin.pluginName
        Dependency.None -> null
      } ?: "Unknown Dependency ID"
    }

}
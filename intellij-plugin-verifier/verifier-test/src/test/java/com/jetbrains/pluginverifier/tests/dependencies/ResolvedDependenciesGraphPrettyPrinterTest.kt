/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

@file:Suppress("TestFunctionName")

package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.pluginverifier.dependencies.*
import com.jetbrains.pluginverifier.dependencies.presentation.ResolvedDependenciesGraphPrettyPrinter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedWriter
import java.io.StringWriter

/**
 * Tests the [ResolvedDependenciesGraphPrettyPrinter].
 */
class ResolvedDependenciesGraphPrettyPrinterTest {

  @Test
  fun `graph without dependencies prints only the verified plugin`() {
    val solo = Node("solo", "1.0")
    val graph = Graph(solo)

    assertEquals("solo:1.0", ResolvedDependenciesGraphPrettyPrinter(graph).prettyPresentation())
  }

  @Test
  fun `missing dependencies precede resolved ones which are sorted by optionality, kind and id`() {
    val root = Node("r", "1.0")
    val graph = Graph(
      root,
      edges = setOf(
        Edge(root, Node("z", "1.0"), Plugin("z")),
        Edge(root, Node("a", "1.0"), Plugin("a", isOptional = true)),
        Edge(root, Node("m", "1.0"), Module("com.m")),
        Edge(root, Node("b", "1.0"), Plugin("b"))
      ),
      missingDependencies = mapOf(
        root to setOf(
          ResolvedMissingDependency(Plugin("y"), "reason y"),
          ResolvedMissingDependency(Plugin("c", isOptional = true), "reason c")
        )
      )
    )

    assertPrettyPresentation(graph, """
      r:1.0
      +--- (failed) c (optional): reason c
      +--- (failed) y: reason y
      +--- b:1.0
      +--- z:1.0
      +--- m:1.0 [declaring module com.m]
      \--- (optional) a:1.0
    """.trimIndent())
  }

  @Test
  fun `repeated node is printed without aliases and children and indentation follows the tree`() {
    val root = Node("r", "1.0", aliases = setOf("r.alias"))
    val a = Node("a", "1.0")
    val b = Node("b", "1.0")
    val platform = Node("p", "1.0", aliases = setOf("p.alias"))
    val graph = Graph(
      root,
      edges = setOf(
        Edge(root, a, Plugin("a")),
        Edge(root, b, Plugin("b")),
        Edge(a, platform, Module("p.alias")),
        Edge(b, platform, Module("p.alias")),
        Edge(platform, Node("leaf", "1.0"), Plugin("leaf"))
      )
    )

    assertPrettyPresentation(graph, """
      r:1.0 (aliased r.alias)
      +--- a:1.0
      |    \--- p:1.0 (aliased p.alias) [declaring module p.alias]
      |         \--- leaf:1.0
      \--- b:1.0
           \--- p:1.0 (*) [declaring module p.alias]
    """.trimIndent())
  }

  @Test
  fun `cyclic dependencies terminate on the already visited node`() {
    val root = Node("r", "1.0")
    val a = Node("a", "1.0")
    val graph = Graph(
      root,
      edges = setOf(
        Edge(root, a, Plugin("a")),
        Edge(a, root, Plugin("r"))
      )
    )

    assertPrettyPresentation(graph, """
      r:1.0
      \--- a:1.0
           \--- r:1.0 (*)
    """.trimIndent())
  }

  @Test
  fun `product modules and content modules are labeled`() {
    val root = Node("r", "1.0")
    val graph = Graph(
      root,
      edges = setOf(
        Edge(root, Node("ide", "1.0", isProductModule = true), Module("com.intellij.modules.product")),
        Edge(root, Node("owner.extra", "2.0", moduleOwnerId = "owner"), Module("owner.extra", isContentModule = true)),
        Edge(root, Node("orphan", "3.0"), Module("orphan", isContentModule = true))
      )
    )

    assertPrettyPresentation(graph, """
      r:1.0
      +--- ide:1.0 [product module]
      +--- orphan:3.0 [content module declared in unknown owner:3.0]
      \--- owner:2.0/owner.extra [content module declared in owner:2.0]
    """.trimIndent())
  }

  @Test
  fun `content module declarations come first and use the diamond connector`() {
    val owner = Node("owner", "1.0")
    val core = Node("owner.core", "1.0", moduleOwnerId = "owner", isContentModuleDeclaration = true)
    val graph = Graph(
      owner,
      edges = setOf(
        Edge(owner, Node("lib", "1.0"), Plugin("lib")),
        Edge(owner, core, Module("owner.core")),
        Edge(core, Node("lib2", "1.0"), Plugin("lib2"))
      ),
      missingDependencies = mapOf(owner to setOf(ResolvedMissingDependency(Plugin("x"), "not found")))
    )

    assertPrettyPresentation(graph, """
      owner:1.0
      ◆--- owner:1.0/owner.core [declared as a content module]
      |    \--- lib2:1.0
      +--- (failed) x: not found
      \--- lib:1.0
    """.trimIndent())
  }

  @Test
  fun `appendable overload writes the same presentation into a writer`() {
    val graph = SampleGraph()
    val expected = ResolvedDependenciesGraphPrettyPrinter(graph).prettyPresentation()

    val stringWriter = StringWriter()
    BufferedWriter(stringWriter).use { writer ->
      ResolvedDependenciesGraphPrettyPrinter(graph).prettyPresentation(writer)
    }

    assertEquals(expected, stringWriter.toString())
  }

  @Test
  fun `appendable overload appends to existing content without a trailing newline`() {
    val out = StringBuilder("prefix|")

    ResolvedDependenciesGraphPrettyPrinter(SampleGraph()).prettyPresentation(out)

    assertEquals("prefix|" + """
      r:1.0
      +--- a:1.0
      |    \--- c:1.0
      \--- b:1.0
           \--- c:1.0 (*)
    """.trimIndent(), out.toString())
  }

  @Test
  fun `printer instance can be used repeatedly`() {
    val printer = ResolvedDependenciesGraphPrettyPrinter(SampleGraph())

    val first = printer.prettyPresentation()
    val second = printer.prettyPresentation()
    val third = StringBuilder().also { printer.prettyPresentation(it) }.toString()

    assertEquals(first, second)
    assertEquals(first, third)
  }

  private fun assertPrettyPresentation(graph: ResolvedDependenciesGraph, expected: String) {
    assertEquals(expected, ResolvedDependenciesGraphPrettyPrinter(graph).prettyPresentation())
    val out = StringBuilder()
    ResolvedDependenciesGraphPrettyPrinter(graph).prettyPresentation(out)
    assertEquals(expected, out.toString())
  }

  private fun SampleGraph(): ResolvedDependenciesGraph {
    val root = Node("r", "1.0")
    val a = Node("a", "1.0")
    val b = Node("b", "1.0")
    val c = Node("c", "1.0")
    return Graph(
      root,
      edges = setOf(
        Edge(root, a, Plugin("a")),
        Edge(root, b, Plugin("b")),
        Edge(a, c, Plugin("c")),
        Edge(b, c, Plugin("c"))
      )
    )
  }

  private fun Node(
    id: String,
    version: String,
    aliases: Set<String> = emptySet(),
    isProductModule: Boolean = false,
    moduleOwnerId: String? = null,
    isContentModuleDeclaration: Boolean = false
  ) = ResolvedDependencyNode(id, version, aliases, isProductModule, moduleOwnerId, isContentModuleDeclaration)

  private fun Plugin(id: String, isOptional: Boolean = false) =
    ResolvedPluginDependency(id, isOptional, isModule = false)

  private fun Module(id: String, isOptional: Boolean = false, isContentModule: Boolean = false) =
    ResolvedPluginDependency(id, isOptional, isModule = true, isContentModule = isContentModule)

  private fun Edge(from: ResolvedDependencyNode, to: ResolvedDependencyNode, dependency: ResolvedPluginDependency) =
    ResolvedDependencyEdge(from, to, dependency)

  private fun Graph(
    verifiedPlugin: ResolvedDependencyNode,
    edges: Set<ResolvedDependencyEdge> = emptySet(),
    missingDependencies: Map<ResolvedDependencyNode, Set<ResolvedMissingDependency>> = emptyMap()
  ): ResolvedDependenciesGraph {
    val vertices = edges.flatMapTo(linkedSetOf(verifiedPlugin)) { listOf(it.from, it.to) }
    return ResolvedDependenciesGraph(verifiedPlugin, vertices, edges, missingDependencies)
  }
}

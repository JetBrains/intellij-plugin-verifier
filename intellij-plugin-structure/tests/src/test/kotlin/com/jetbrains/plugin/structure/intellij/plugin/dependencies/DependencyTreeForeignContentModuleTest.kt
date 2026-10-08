/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val PROVIDER_ID = "com.example.Provider"
private const val PROVIDER_CORE = "com.example.Provider.core"
private const val PROVIDER_EXTRAS = "com.example.Provider.extras"
private const val MAIN_DEPENDENCY_ID = "com.example.MainDependency"
private const val CORE_DEPENDENCY_ID = "com.example.CoreDependency"
private const val EXTRAS_DEPENDENCY_ID = "com.example.ExtrasDependency"
private const val SOME_PLUGIN_ID = "com.example.Consumer"

/**
 * A plugin depends on a single content module (`core`) of a foreign plugin that declares
 * two content modules (`core` and `extras`).
 */
class DependencyTreeForeignContentModuleTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var provider: IdePlugin
  private lateinit var somePlugin: IdePlugin
  private lateinit var ide: MockIde

  @Before
  fun setUp() {
    val mainDependency = buildPlugin(MAIN_DEPENDENCY_ID)
    val coreDependency = buildPlugin(CORE_DEPENDENCY_ID)
    val extrasDependency = buildPlugin(EXTRAS_DEPENDENCY_ID)
    provider = buildPlugin(
      PROVIDER_ID,
      body = """
        <depends>$MAIN_DEPENDENCY_ID</depends>
        <content>
          <module name="$PROVIDER_CORE" loading="required"/>
          <module name="$PROVIDER_EXTRAS" loading="required"/>
        </content>
      """
    ) {
      zip("core.jar") {
        file("$PROVIDER_CORE.xml", moduleDescriptor(CORE_DEPENDENCY_ID))
      }
      zip("extras.jar") {
        file("$PROVIDER_EXTRAS.xml", moduleDescriptor(EXTRAS_DEPENDENCY_ID))
      }
    }
    somePlugin = buildPlugin(
      SOME_PLUGIN_ID,
      body = """
        <dependencies>
          <module name="$PROVIDER_CORE"/>
        </dependencies>
      """
    )
    ide = MockIde(
      IdeVersion.createIdeVersion("IU-251.6125"),
      temporaryFolder.newFolder("idea").toPath(),
      listOf(provider, mainDependency, coreDependency, extrasDependency)
    )
  }

  @Test
  fun `content module of a foreign plugin does not own its sibling content modules`() {
    val dependencyTree = DependencyTree(ide)
    val resolution = dependencyTree.getDependencyTreeResolution(somePlugin)

    val coreNode = NodeId(PROVIDER_ID, PROVIDER_CORE)
    val edgesFromCore = mutableListOf<Pair<Dependency, Dependency>>()
    resolution.forEach { from, to ->
      if (from.nodeId == coreNode) edgesFromCore += from to to
    }
    assertTrue(
      "Expected '$PROVIDER_CORE' to be resolved with an edge to '$CORE_DEPENDENCY_ID', but got $edgesFromCore",
      edgesFromCore.any { (_, to) -> to.nodeId == NodeId(CORE_DEPENDENCY_ID, null) }
    )
    val edgesFromCoreToProvider = edgesFromCore.filter { (_, to) -> to.nodeId?.pluginId == PROVIDER_ID }
    assertEquals(
      "Content module '$PROVIDER_CORE' must not have edges to other nodes of its own plugin '$PROVIDER_ID'",
      emptyList<Pair<Dependency, Dependency>>(),
      edgesFromCoreToProvider
    )
    assertEquals(setOf(NodeId(CORE_DEPENDENCY_ID, null)), edgesFromCore.map { it.second.nodeId }.toSet())
    assertEquals(coreOnlyEdges(), graphEdges(resolution))
    val expectedDependencies = setOf(
      Dependency.Module(provider, PROVIDER_CORE),
      Dependency.Plugin(externalPlugin(CORE_DEPENDENCY_ID), isTransitive = true)
    )
    assertEquals(expectedDependencies, resolution.transitiveDependencies.toSet())
    assertEquals(expectedDependencies, dependencyTree.getTransitiveDependencies(somePlugin))
  }

  @Test
  fun `explicit sibling dependency expands extras without reaching provider main`() {
    provider = buildProvider(coreDependencies = "<module name=\"$PROVIDER_EXTRAS\"/>")
    val resolution = dependencyTree().getDependencyTreeResolution(somePlugin)
    assertEquals(coreOnlyEdges() + mapOf(
      NodeId(PROVIDER_ID, PROVIDER_CORE) to setOf(NodeId(CORE_DEPENDENCY_ID, null), NodeId(PROVIDER_ID, PROVIDER_EXTRAS)),
      NodeId(PROVIDER_ID, PROVIDER_EXTRAS) to setOf(NodeId(EXTRAS_DEPENDENCY_ID, null))
    ), graphEdges(resolution))
    assertTrue(resolution.missingDependencies.isEmpty())
    assertDependencies(setOf(
      Dependency.Module(provider, PROVIDER_CORE),
      Dependency.Plugin(externalPlugin(CORE_DEPENDENCY_ID), isTransitive = true),
      Dependency.Plugin(externalPlugin(EXTRAS_DEPENDENCY_ID), isTransitive = true)
    ))
  }

  @Test
  fun `explicit sibling cycle terminates and contains only declared edges`() {
    provider = buildProvider(
      coreDependencies = "<module name=\"$PROVIDER_EXTRAS\"/>",
      extrasDependencies = "<module name=\"$PROVIDER_CORE\"/>"
    )
    val resolution = dependencyTree().getDependencyTreeResolution(somePlugin)
    assertEquals(coreOnlyEdges() + mapOf(
      NodeId(PROVIDER_ID, PROVIDER_CORE) to setOf(NodeId(CORE_DEPENDENCY_ID, null), NodeId(PROVIDER_ID, PROVIDER_EXTRAS)),
      NodeId(PROVIDER_ID, PROVIDER_EXTRAS) to setOf(NodeId(EXTRAS_DEPENDENCY_ID, null), NodeId(PROVIDER_ID, PROVIDER_CORE))
    ), graphEdges(resolution))
    assertDependencies(setOf(
      Dependency.Module(provider, PROVIDER_CORE),
      Dependency.Plugin(externalPlugin(CORE_DEPENDENCY_ID), isTransitive = true),
      Dependency.Plugin(externalPlugin(EXTRAS_DEPENDENCY_ID), isTransitive = true)
    ))
  }

  @Test
  fun `main only and mixed entry orders expose exactly their reachable dependencies`() {
    val mainEntry = "<plugin id=\"$PROVIDER_ID\"/>"
    val coreEntry = "<module name=\"$PROVIDER_CORE\"/>"
    val extrasEntry = "<module name=\"$PROVIDER_EXTRAS\"/>"
    val externalDependencies = setOf(
      Dependency.Plugin(externalPlugin(MAIN_DEPENDENCY_ID), isTransitive = true),
      Dependency.Plugin(externalPlugin(CORE_DEPENDENCY_ID), isTransitive = true),
      Dependency.Plugin(externalPlugin(EXTRAS_DEPENDENCY_ID), isTransitive = true)
    )
    for (entries in listOf(
      listOf(mainEntry),
      listOf(mainEntry, coreEntry, extrasEntry),
      listOf(coreEntry, extrasEntry, mainEntry)
    )) {
      somePlugin = buildPlugin(SOME_PLUGIN_ID, "<dependencies>${entries.joinToString("")}</dependencies>")
      val expected = externalDependencies + Dependency.Plugin(provider) +
        (if (coreEntry in entries) setOf(Dependency.Module(provider, PROVIDER_CORE), Dependency.Module(provider, PROVIDER_EXTRAS)) else emptySet())
      assertDependencies(expected)
      assertEquals(mapOf(
        NodeId(SOME_PLUGIN_ID, null) to entries.map {
          when (it) {
            mainEntry -> NodeId(PROVIDER_ID, null)
            coreEntry -> NodeId(PROVIDER_ID, PROVIDER_CORE)
            else -> NodeId(PROVIDER_ID, PROVIDER_EXTRAS)
          }
        }.toSet(),
        NodeId(PROVIDER_ID, null) to setOf(NodeId(PROVIDER_ID, PROVIDER_CORE), NodeId(PROVIDER_ID, PROVIDER_EXTRAS), NodeId(MAIN_DEPENDENCY_ID, null)),
        NodeId(PROVIDER_ID, PROVIDER_CORE) to setOf(NodeId(CORE_DEPENDENCY_ID, null)),
        NodeId(PROVIDER_ID, PROVIDER_EXTRAS) to setOf(NodeId(EXTRAS_DEPENDENCY_ID, null))
      ), graphEdges(dependencyTree().getDependencyTreeResolution(somePlugin)))
    }
  }

  @Test
  fun `core and extras entries without main do not resolve main dependencies`() {
    somePlugin = buildPlugin(SOME_PLUGIN_ID, """
      <dependencies>
        <module name="$PROVIDER_CORE"/>
        <module name="$PROVIDER_EXTRAS"/>
      </dependencies>
    """)
    assertDependencies(setOf(
      Dependency.Module(provider, PROVIDER_CORE),
      Dependency.Module(provider, PROVIDER_EXTRAS),
      Dependency.Plugin(externalPlugin(CORE_DEPENDENCY_ID), isTransitive = true),
      Dependency.Plugin(externalPlugin(EXTRAS_DEPENDENCY_ID), isTransitive = true)
    ))
    assertEquals(coreOnlyEdges() + mapOf(
      NodeId(SOME_PLUGIN_ID, null) to setOf(NodeId(PROVIDER_ID, PROVIDER_CORE), NodeId(PROVIDER_ID, PROVIDER_EXTRAS)),
      NodeId(PROVIDER_ID, PROVIDER_EXTRAS) to setOf(NodeId(EXTRAS_DEPENDENCY_ID, null))
    ), graphEdges(dependencyTree().getDependencyTreeResolution(somePlugin)))
  }

  @Test
  fun `direct dependency wins over its transitive occurrence in both entry orders`() {
    for (entries in listOf(
      "<module name=\"$PROVIDER_CORE\"/><plugin id=\"$CORE_DEPENDENCY_ID\"/>",
      "<plugin id=\"$CORE_DEPENDENCY_ID\"/><module name=\"$PROVIDER_CORE\"/>"
    )) {
      somePlugin = buildPlugin(SOME_PLUGIN_ID, "<dependencies>$entries</dependencies>")
      assertDependencies(setOf(Dependency.Module(provider, PROVIDER_CORE), Dependency.Plugin(externalPlugin(CORE_DEPENDENCY_ID))))
    }
  }

  @Test
  fun `unreachable missing main and sibling declarations are not reported`() {
    provider = buildProvider(
      mainDependencies = "<depends>missing.main</depends>",
      extrasDependencies = "<plugin id=\"missing.extras\"/>"
    )
    val tree = dependencyTree()
    val resolution = tree.getDependencyTreeResolution(somePlugin)
    assertEquals(coreOnlyEdges(), graphEdges(resolution))
    assertTrue(resolution.missingDependencies.isEmpty())
    val missing = mutableSetOf<String>()
    tree.getTransitiveDependencies(somePlugin, { _, dep -> missing += dep.id })
    assertTrue(missing.isEmpty())
  }

  @Test
  fun `reachable missing module dependencies are reported for their owner`() {
    provider = buildProvider(coreDependencies = "<plugin id=\"missing.core\"/>")
    val tree = dependencyTree()
    val resolution = tree.getDependencyTreeResolution(somePlugin)
    assertEquals(coreOnlyEdges(), graphEdges(resolution))
    assertEquals(mapOf(provider to setOf("missing.core")), resolution.missingDependencies.mapValues { (_, deps) -> deps.map { it.id }.toSet() })
    val missing = mutableSetOf<Pair<IdePlugin, String>>()
    tree.getTransitiveDependencies(somePlugin, { owner, dep -> missing += owner to dep.id })
    assertEquals(setOf(provider to "missing.core"), missing)
  }

  private fun coreOnlyEdges() = mapOf(
    NodeId(SOME_PLUGIN_ID, null) to setOf(NodeId(PROVIDER_ID, PROVIDER_CORE)),
    NodeId(PROVIDER_ID, PROVIDER_CORE) to setOf(NodeId(CORE_DEPENDENCY_ID, null))
  )

  private fun externalPlugin(id: String) = ide.getBundledPlugins().single { it.pluginId == id }

  private fun assertDependencies(expected: Set<Dependency>) {
    val tree = dependencyTree()
    val resolution = tree.getDependencyTreeResolution(somePlugin)
    assertTrue(resolution.missingDependencies.isEmpty())
    assertEquals(expected, resolution.transitiveDependencies.toSet())
    assertEquals(expected, tree.getTransitiveDependencies(somePlugin))
  }

  private fun graphEdges(resolution: DependencyTreeResolution): Map<NodeId, Set<NodeId>> {
    val edges = linkedMapOf<NodeId, MutableSet<NodeId>>()
    resolution.forEach { from, to ->
      edges.getOrPut(requireNotNull(from.nodeId)) { linkedSetOf() } += requireNotNull(to.nodeId)
    }
    return edges
  }

  private fun dependencyTree() = DependencyTree(MockIde(
    ide.getVersion(), ide.getIdePath(), listOf(provider) + ide.getBundledPlugins().filter { it.pluginId != PROVIDER_ID }
  ))

  private fun buildProvider(
    mainDependencies: String = "<depends>$MAIN_DEPENDENCY_ID</depends>",
    coreDependencies: String = "",
    extrasDependencies: String = ""
  ) = buildPlugin(PROVIDER_ID, """
    $mainDependencies
    <content>
      <module name="$PROVIDER_CORE" loading="required"/>
      <module name="$PROVIDER_EXTRAS" loading="required"/>
    </content>
  """) {
    zip("core.jar") { file("$PROVIDER_CORE.xml", moduleDescriptor(CORE_DEPENDENCY_ID, coreDependencies)) }
    zip("extras.jar") { file("$PROVIDER_EXTRAS.xml", moduleDescriptor(EXTRAS_DEPENDENCY_ID, extrasDependencies)) }
  }

  private fun moduleDescriptor(pluginDependencyId: String, additionalDependencies: String = "") = """
    <idea-plugin>
      <dependencies>
        <plugin id="$pluginDependencyId"/>
        $additionalDependencies
      </dependencies>
    </idea-plugin>
  """.trimIndent()

  private fun buildPlugin(id: String, body: String = "", additionalContent: ContentBuilder.() -> Unit = {}): IdePlugin {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().toPath().resolve("$id.zip")) {
      dir(id) {
        dir("lib") {
          zip("$id.jar") {
            dir("META-INF") {
              file(
                "plugin.xml",
                """
                  <idea-plugin>
                    <id>$id</id>
                    <name>$id</name>
                    <version>1.0</version>
                    <vendor>JetBrains</vendor>
                    <description>Plugin used to verify dependency tree resolution of foreign content modules.</description>
                    <change-notes>Plugin used to verify dependency tree resolution of foreign content modules.</change-notes>
                    <idea-version since-build="241.0"/>
                    $body
                  </idea-plugin>
                """.trimIndent()
              )
            }
          }
          additionalContent()
        }
      }
    }
    val creationResult = IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true)
    if (creationResult !is PluginCreationSuccess) {
      fail("Expected successful creation of '$id', but got $creationResult")
    }
    return (creationResult as PluginCreationSuccess).plugin
  }
}

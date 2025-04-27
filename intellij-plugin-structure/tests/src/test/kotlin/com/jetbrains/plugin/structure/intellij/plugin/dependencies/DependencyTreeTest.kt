package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class DependencyTreeTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  private lateinit var tenIjDependencies: List<MockIdePlugin>

  private lateinit var ijPlugin: MockIdePlugin

  private lateinit var dozenOfPlugins: List<MockIdePlugin>

  private lateinit var pluginAlpha: MockIdePlugin

  private lateinit var ide: MockIde

  private lateinit var pluginNotInIde: MockIdePlugin

  private lateinit var somePlugin: MockIdePlugin

  @Before
  fun setUp() {
    ideRoot = temporaryFolder.newFolder("idea").toPath()

    tenIjDependencies = (1..10).map {
      MockIdePlugin(pluginId = "ij-dependency-$it")
    }

    ijPlugin = MockIdePlugin("ij", dependencies = tenIjDependencies.map { dependOn(it.pluginId!!) })

    dozenOfPlugins = (1..12).map {
      MockIdePlugin(pluginId = "plugin$it", dependencies = listOf(dependOn("ij")))
    }

    pluginAlpha = MockIdePlugin(pluginId = "alpha", dependencies =
      dozenOfPlugins.map { dependOn(it.pluginId!!) }
    )

    ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, listOf(pluginAlpha, ijPlugin) + dozenOfPlugins + tenIjDependencies)

    pluginNotInIde = MockIdePlugin(pluginId = "notInIde", dependencies = listOf(dependOn("pluginAlpha")))

    somePlugin = MockIdePlugin(pluginId = "com.example.A", dependencies = listOf(dependOn("alpha"), dependOn("pluginNotInIde")))
  }



  @Test
  fun `dependency tree is correct`() {
    val dependencyTree = DependencyTree(ide)
    val expectedDependencies = setOf(
      Dependency.Plugin(pluginAlpha)) +
      // pluginNotInIde is not in the IDE, has been excluded
      dozenOfPlugins.map { Dependency.Plugin(it, isTransitive = true) } +
      Dependency.Plugin(ijPlugin, isTransitive = true) +
      tenIjDependencies.map { Dependency.Plugin(it, isTransitive = true) }

    val actualDependencies = dependencyTree.getTransitiveDependencies(somePlugin)
    assertEquals(expectedDependencies, actualDependencies)
  }

  private fun dependOn(id: String): PluginDependencyImpl {
    return PluginDependencyImpl(id, false, false)
  }
}
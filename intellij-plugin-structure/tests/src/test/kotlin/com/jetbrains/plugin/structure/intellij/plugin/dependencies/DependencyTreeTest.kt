package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DependencyTreeTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `dependency tree is correct`() {
    val ideRoot = temporaryFolder.newFolder("idea").toPath()

    val tenIjDependencies = (1..10).map {
      MockIdePlugin(pluginId = "ij-dependency-$it")
    }

    val ijPlugin = MockIdePlugin("ij", dependencies = tenIjDependencies.map { dependOn(it.pluginId!!) })

    val dozenOfPlugins = (1..12).map {
      MockIdePlugin(pluginId = "plugin$it", dependencies = listOf(dependOn("ij")))
    }


    val pluginAlpha = MockIdePlugin(pluginId = "alpha", dependencies =
      dozenOfPlugins.map { dependOn(it.pluginId!!) }
    )

    val pluginNotInIde = MockIdePlugin(pluginId = "notInIde", dependencies = listOf(dependOn("pluginAlpha")))
    val somePlugin = MockIdePlugin(pluginId = "com.example.A", dependencies = listOf(dependOn("alpha"), dependOn("pluginNotInIde")))

    val ide = MockIde(IdeVersion.createIdeVersion("IU-251.6125"), ideRoot, listOf(pluginAlpha, ijPlugin) + dozenOfPlugins + tenIjDependencies)

    val dependencyTree = DependencyTree(ide)
    println(dependencyTree.getTransitiveDependencies(somePlugin))
  }

  private fun dependOn(id: String): PluginDependencyImpl {
    return PluginDependencyImpl(id, false, false)
  }
}
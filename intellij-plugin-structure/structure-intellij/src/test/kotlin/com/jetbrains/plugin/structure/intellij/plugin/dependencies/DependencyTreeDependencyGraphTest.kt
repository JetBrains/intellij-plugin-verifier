/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    graph.addEdge(somePluginId, Dependency.Plugin(alphaPlugin))
    graph.addEdge(somePluginId, Dependency.Plugin(betaPlugin))
    graph.addEdge(alphaPluginId, Dependency.Plugin(comIntellijPlugin))
    graph.addEdge(betaPluginId, Dependency.Plugin(comIntellijPlugin))

    val vertices = mutableSetOf<PluginId>()
    graph.forEachAdjacency { from, dependencies ->
      vertices.add(from.id)
      dependencies.forEach {
        vertices += it.id
      }
    }
    assertEquals(setOf(somePluginId, alphaPluginId, betaPluginId, comIntellijPluginId), vertices)

    val somePluginDependsOnAlpha = graph.contains(somePluginId) {
      it.matches(alphaPluginId)
    }
    assertTrue(somePluginDependsOnAlpha)
  }

  // FIXME Duplicate from com.jetbrains.plugin.structure.ide.classes.resolver.CachingPluginDependencyResolverProvider.getPluginId
  private val Dependency.id: String
    get() {
      return when (this) {
        is Dependency.Module -> this.plugin.pluginId ?: this.plugin.pluginName
        is Dependency.Plugin -> this.plugin.pluginId ?: this.plugin.pluginName
        Dependency.None -> null
      } ?: "Unknown Dependency ID"
    }

}
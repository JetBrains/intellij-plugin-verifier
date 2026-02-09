/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DependencyResolutionTest {
  @Test
  fun `dependency resolution is correct`() {
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

    val dependencyGraph = DependencyTree.DependencyGraph(Dependency.Plugin(somePlugin)).apply {
      addEdge(somePluginId, Dependency.Plugin(alphaPlugin))
      addEdge(somePluginId, Dependency.Plugin(betaPlugin))
      addEdge(alphaPluginId, Dependency.Plugin(comIntellijPlugin))
      addEdge(betaPluginId, Dependency.Plugin(comIntellijPlugin))
    }

    val transitiveDependencies = mutableSetOf<Dependency>()
    dependencyGraph.forEachAdjacency { pluginId, dependencies ->
      transitiveDependencies += dependencies
    }

    data class Edge(val from: PluginId, val to: PluginId, val dependency: PluginDependency)

    val edges = mutableListOf<Edge>()
    val dependencyTreeResolution = DefaultDependencyTreeResolution(somePlugin, transitiveDependencies, missingDependencies = emptyMap(), dependencyGraph)
    dependencyTreeResolution.forEach { from, dependency ->
      edges += Edge(from.id, dependency.id, dependency.pluginDependency!!)
    }

    val expectedEdges = mutableListOf<Edge>().apply {
      add(Edge(somePluginId, alphaPluginId, PluginDependencyImpl(alphaPluginId, false, false)))
      add(Edge(somePluginId, betaPluginId, PluginDependencyImpl(betaPluginId, false, false)))
      add(Edge(alphaPluginId, comIntellijPluginId, PluginDependencyImpl(comIntellijPluginId, false, false)))
      add(Edge(betaPluginId, comIntellijPluginId, PluginDependencyImpl(comIntellijPluginId, false, false)))
    }

    assertEquals(expectedEdges, edges)
  }

  @Test
  fun `dependency on module provided by plugin is resolved`() {
    // Module 'com.intellij.modules.vcs' provided by plugin 'intellij.platform.vcs.impl'
    val somePluginId = "com.example.SomePlugin"
    val somePlugin = mockk<IdePlugin>()
    every { somePlugin.pluginId } returns somePluginId

    val vcsModuleId = "com.intellij.modules.vcs"
    val vcsImplPluginId = "intellij.platform.vcs.impl"
    val vcsImpl = mockk<IdePlugin>()
    every { vcsImpl.pluginId } returns vcsImplPluginId
    every { vcsImpl.definedModules } returns setOf(vcsModuleId)

    val dependencyGraph = DependencyTree.DependencyGraph(Dependency.Plugin(somePlugin)).apply {
      addEdge(somePluginId, Dependency.Module(vcsImpl, vcsModuleId, false))
    }

    dependencyGraph.forEachAdjacency { pluginId, dependencies ->
      assertEquals(1, dependencies.size)
      val d = dependencies.single()
      assertEquals(Dependency.Module(vcsImpl, vcsModuleId, false), d)
    }
  }
}
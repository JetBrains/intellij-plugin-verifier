/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.dependencies.processing

import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import org.jgrapht.Graph
import org.jgrapht.alg.cycle.JohnsonSimpleCycles
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

data class DependenciesGraphCycleFinder(val dependenciesGraph: DependenciesGraph) {

  fun findAllCycles(): List<List<DependencyNode>> {
    val graph: Graph<DependencyNode, DefaultEdge> = DefaultDirectedGraph(DefaultEdge::class.java)
    dependenciesGraph.vertices.forEach { graph.addVertex(it) }
    dependenciesGraph.edges.forEach { graph.addEdge(it.from, it.to) }
    //TODO: solve the problem here actually
    return try {
      JohnsonSimpleCycles(graph).findSimpleCycles()
    } catch (e: Exception) {
      emptyList()
    }
  }

}
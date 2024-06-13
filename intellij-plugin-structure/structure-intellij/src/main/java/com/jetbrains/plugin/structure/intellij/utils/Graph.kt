package com.jetbrains.plugin.structure.intellij.utils

class Graph<T> {

  private val adjacencyMap = mutableMapOf<T, MutableList<T>>()

  fun addEdge(from: T, to: T): Edge<T> {
    val edge = Edge(from, to)
    val adjacentVertices = adjacencyMap.getOrPut(from) { mutableListOf() }
    adjacentVertices.add(to)
    return edge
  }

  operator fun plusAssign(edge: Pair<T, T>) {
    addEdge(edge.first, edge.second)
  }

  override fun toString(): String = buildString {
    adjacencyMap.forEach { (vertex, adjacents) ->
      val a = adjacents.joinToString()
      append("$vertex -> [$a];")
    }
  }

  fun hasEdgeFrom(from: T): Boolean = adjacencyMap.containsKey(from)

  val edgeCount: Int
    get() = adjacencyMap.size

  val nodeCount: Int
    get() {
      return adjacencyMap.flatMap { (module, deps) -> deps + module }
        .toSet().size
    }

  data class Edge<T>(val from: T, val to: T)
}


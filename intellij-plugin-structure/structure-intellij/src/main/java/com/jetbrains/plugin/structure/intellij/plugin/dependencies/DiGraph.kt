package com.jetbrains.plugin.structure.intellij.plugin.dependencies

internal class DiGraph<I, O> {
  private val adjacency = hashMapOf<I, MutableList<O>>()

  operator fun get(from: I): List<O> = adjacency[from] ?: emptyList()

  fun addEdge(from: I, to: O) {
    adjacency.getOrPut(from) { mutableListOf() } += to
  }

  fun contains(from: I, to: O): Boolean = adjacency[from]?.contains(to) == true

  fun contains(from: I, to: I, propertyExtractor: (O) -> I?): Boolean {
    val adj = adjacency[from] ?: emptyList<O>()
    return adj.map { propertyExtractor(it) }.contains(to)
  }
}
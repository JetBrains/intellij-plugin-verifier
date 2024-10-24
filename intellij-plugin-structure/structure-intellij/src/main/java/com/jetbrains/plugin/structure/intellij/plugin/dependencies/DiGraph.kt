package com.jetbrains.plugin.structure.intellij.plugin.dependencies

internal class DiGraph<I, O> {
  private val adjacents = hashMapOf<I, MutableList<O>>()

  operator fun get(from: I): List<O> = adjacents[from] ?: emptyList()

  fun addEdge(from: I, to: O) {
    adjacents.getOrPut(from) { mutableListOf() } += to
  }

  fun contains(from: I, to: O): Boolean = adjacents[from]?.contains(to) == true

  fun contains(from: I, to: I, propertyExtractor: (O) -> I?): Boolean {
    val adj = adjacents[from] ?: emptyList<O>()
    return adj.map { propertyExtractor(it) }.contains(to)
  }
}
/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

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
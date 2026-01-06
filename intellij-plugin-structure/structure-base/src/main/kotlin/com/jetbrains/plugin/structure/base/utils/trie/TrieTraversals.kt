/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.trie

import org.jetbrains.annotations.TestOnly

object TrieTraversals {

  class Leaves<V>: Trie.NodeVisitor<V, Unit> {
    val result = mutableSetOf<CharSequence>()
    override fun visit(word: CharSequence, value: V?, isLeaf: Boolean, isTerminal: Boolean) {
      if (isLeaf) result += word.toString()
    }
  }

  @TestOnly
  fun <V> Trie<V>.leafCount(): Int = visit { _, _, isLeaf, _ -> if (isLeaf) 1 else 0 }.sum()

  @TestOnly
  fun <V> Trie<V>.nodeCount(): Int = visit { _, _, _, _ -> 1 }.sum()

  @TestOnly
  fun <V> Trie<V>.valueCount(expectedValue: V): Int = visit { _, value, _, _ ->
    if (expectedValue == value) 1 else 0
  }.sum()

  fun <V> Trie<V>.withNonNullValues(): Set<String> = visit { prefix, value, _, _ ->
    prefix.takeIf { value != null }?.toString()
  }.filterNotNullTo(mutableSetOf())

  fun <V> Trie<V>.withValue(expectedValue: V?): Set<String> = visit { prefix, value, _, _ ->
    prefix.takeIf { value == expectedValue }?.toString()
  }.filterNotNullTo(mutableSetOf())

  fun <V> Trie<V>.getInsertions(): Set<String> = mutableSetOf<String>().apply {
    visit { prefix, _, _, isTerminal ->
      if (isTerminal) add(prefix.toString())
    }
  }

  fun <V> Trie<V>.getAllNodes(): Set<String> {
    val results = mutableSetOf<String>()
    visit { prefix, _, _, isTerminal ->
      if (isTerminal || prefix.isNotEmpty()) {
        results.add(prefix.toString())
      }
      Unit
    }
    return results
  }
}

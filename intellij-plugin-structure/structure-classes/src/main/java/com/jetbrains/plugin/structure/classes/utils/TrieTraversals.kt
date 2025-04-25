package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.classes.utils.Trie.NodeVisitor
import com.jetbrains.plugin.structure.classes.utils.Trie.NodeVisitor.NodeVisit

object TrieTraversals {

  class Leaves<V>: NodeVisitor<V, Unit> {
    val result = mutableSetOf<CharSequence>()

    override fun visit(n: NodeVisit<V>) {
      if (n.isLeaf) result += n.word.toString()
    }
  }

  fun <V> Trie<V>.leafCount(): Int = visit { if (it.isLeaf) 1 else 0 }.sum()

  fun <V> Trie<V>.nodeCount(): Int = visit { 1 }.sum()

  fun <V> Trie<V>.valueCount(expectedValue: V): Int = visit { (_, value, _) ->
    if (expectedValue == value) 1 else 0
  }.sum()

  fun <V> Trie<V>.withNonNullValues(): Set<String> = visit { (prefix, value, _) ->
    prefix.takeIf { value != null }?.toString()
  }.filterNotNullTo(mutableSetOf<String>())

  fun <V> Trie<V>.withValue(expectedValue: V?): Set<String> = visit { (prefix, value, _) ->
    prefix.takeIf { value == expectedValue }?.toString()
  }.filterNotNullTo(mutableSetOf<String>())

  fun <V> Trie<V>.getInsertions(): Set<String> = mutableSetOf<String>().apply {
    visit { (prefix, _, _, isTerminal) ->
      if (isTerminal) add(prefix.toString())
    }
  }

  fun <V> Trie<V>.withDelimiter(delimiter: Char): Set<String> = mutableSetOf<String>().apply {
    visit { (prefix, value, _, isTerminal) ->
      if (isTerminal) {
        add(prefix.toString())
      } else if (prefix.isNotEmpty() && prefix.last() == delimiter) {
        add(prefix.subSequence(0, prefix.lastIndex).toString())
      }
      value
    }
  }
}
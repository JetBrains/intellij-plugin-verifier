package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.classes.utils.Trie.Visitor

object TrieTraversals {
  fun <V> Trie<V>.collect(wordSeparator: Char, wordCollector: SetCollector<V>): Set<String> {
    visit(wordSeparator, wordCollector)
    return wordCollector.result
  }

  abstract class SetCollector<V> : Visitor<V> {
    protected val _result = mutableSetOf<String>()
    val result: Set<String> get() = _result
  }

  class LeafCount<V> : Visitor<V> {
    private var _count = 0
    val count get() = _count

    override fun visit(word: String, value: V?, isLeaf: Boolean) {
      if (isLeaf) _count++
    }
  }

  class All<V>: SetCollector<V>() {
    override fun visit(word: String, value: V?, isLeaf: Boolean) {
      _result += word
    }
  }

  class Leaves<V>: SetCollector<V>() {
    override fun visit(word: String, value: V?, isLeaf: Boolean) {
      if (isLeaf) _result += word
    }
  }

  class WithValue<V>(private val expectedValue: V) : SetCollector<V>() {
    override fun visit(word: String, value: V?, isLeaf: Boolean) {
      if (expectedValue == value) _result += word
    }
  }

}
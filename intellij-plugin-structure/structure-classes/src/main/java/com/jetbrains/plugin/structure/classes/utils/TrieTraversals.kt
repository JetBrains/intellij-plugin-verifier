package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.classes.utils.Trie.Visitor

object TrieTraversals {
  class All<V>: Visitor<V> {
    private val _result = mutableSetOf<String>()
    val result: Set<String> get() = _result

    override fun visit(word: String, value: V?, isLeaf: Boolean) {
      _result += word
    }
  }

  class Leaves<V>: Visitor<V> {
    private val _result = mutableSetOf<String>()
    val result: Set<String> get() = _result

    override fun visit(word: String, value: V?, isLeaf: Boolean) {
      if (isLeaf) _result += word
    }
  }

  class WithValue<V>(private val expectedValue: V) : Visitor<V> {
    private val _result = mutableSetOf<String>()
    val result: Set<String> get() = _result

    override fun visit(word: String, value: V?, isLeaf: Boolean) {
      if (expectedValue == value) _result += word
    }
  }

}
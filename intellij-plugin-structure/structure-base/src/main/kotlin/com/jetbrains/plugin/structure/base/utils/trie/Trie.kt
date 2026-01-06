/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.trie


class Trie<V> {
  private data class Node<V>(
    val children: MutableMap<String, Node<V>> = HashMap(),
    var childrenSeparator: Char? = null,
    var value: V? = null,
    var isTerminal: Boolean = false
  )

  private fun empty() = Node<V>()

  private val root = empty()

  val isEmpty: Boolean get() = root.isChildless && !root.isTerminal
  private val Node<V>.isChildless: Boolean get() = children.isEmpty()

  fun contains(prefix: CharSequence): Boolean = findNode(prefix) != null

  fun findValue(prefix: CharSequence): V? = findNode(prefix)?.value

  private fun findNode(prefix: CharSequence): Node<V>? {
    var n = root
    for (pair in split(prefix)) {
      if (n.childrenSeparator != pair.first) return null
      n = n.children[pair.second] ?: return null
    }
    return n
  }

  fun insert(key: CharSequence, value: V? = null): Boolean {
    var isInserted = false
    var currentNode = root
    for (pair in split(key)) {
      if (currentNode.isChildless) currentNode.childrenSeparator = pair.first
      else if (currentNode.childrenSeparator != pair.first) {
        throw IllegalArgumentException("Node '$currentNode' has separator '${currentNode.childrenSeparator}' but trying to insert child '${pair.second}' with separator '${pair.first}'")
      }
      currentNode = currentNode.children.getOrPut(pair.second) { empty().also { isInserted = true } }
    }
    currentNode.value = value
    currentNode.isTerminal = true
    return isInserted
  }

  fun <R> visit(visitor: NodeVisitor<V, R>): List<R> {
    if (isEmpty) return emptyList()
    val result = mutableListOf<R>()
    visit(root, StringBuilder(), result, visitor)
    return result
  }

  private fun <R> visit(node: Node<V>, prefix: StringBuilder, result: MutableList<R>, visitor: NodeVisitor<V, R>) {
    if (node.isChildless) {
      result += visitor.visit(prefix, node.value, isLeaf = true, node.isTerminal)
    } else {
      result += visitor.visit(prefix, node.value, isLeaf = false, node.isTerminal)
      for ((edge, child) in node.children) {
        with(prefix) {
          val mark = length
          if (isNotEmpty()) {
            append(node.childrenSeparator)
          }
          append(edge)
          visit(child, this, result, visitor)
          delete(mark, length)
        }
      }
    }
  }

  fun interface NodeVisitor<V, R> {
    fun visit(word: CharSequence, value: V?, isLeaf: Boolean, isTerminal: Boolean): R
  }

  companion object {
    fun split(str: CharSequence): List<Pair<Char?, String>> {
      if (str.isEmpty()) return emptyList()
      val result = ArrayList<Pair<Char?, String>>()
      var separator: Char? = null
      var i = 0
      // trim start separators
      while (str[i] == '/' || str[i] == '.' || str[i] == '$') {
        i++
      }
      var startIndex = i
      while (i < str.length) {
        val c = str[i]
        if (c == '/' || c == '.' || c == '$') {
          if (startIndex != i) {
            result.add(Pair(separator, str.substring(startIndex, i)))
          } else if (startIndex != 0) {
            i++
            continue
          }
          separator = c
          startIndex = i + 1
        }
        i++
      }
      if (startIndex < str.length) {
        result.add(Pair(separator, str.substring(startIndex, str.length)))
      }
      if (result.isNotEmpty()) {
        assert(result.first().first == null)
      }
      return result
    }
  }
}




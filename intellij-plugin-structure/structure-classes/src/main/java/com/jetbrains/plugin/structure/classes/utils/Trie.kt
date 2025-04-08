package com.jetbrains.plugin.structure.classes.utils


class Trie<V>(val defaultValue: V? = null) {
  private data class Node<V>(
    val children: MutableMap<Char, Node<V>> = mutableMapOf(),
    var value: V? = null
  )

  private fun empty(value: V?) = Node<V>(mutableMapOf(), value)

  private val root = empty(defaultValue)

  val isEmpty: Boolean get() = root.children.isEmpty()

  fun find(word: CharSequence): Boolean {
    var n = root
    for (c in word) {
      n = n.children[c] ?: return false
    }
    return true
  }

  fun insert(key: CharSequence, value: V? = defaultValue): Boolean {
    var isInserted = false
    var currentNode = root
    for (char in key) {
      currentNode = currentNode.children.getOrPut(char) { empty(defaultValue).also { isInserted = true } }
    }
    currentNode.value = value
    return isInserted
  }

  fun findAllWords(value: V?, wordSeparator: Char = '.'): Set<String> {
    return mutableSetOf<String>().apply {
      visit(root, "", wordSeparator) { word, visitedValue, leaf ->
        if (leaf || value == visitedValue) {
          add(word)
        }
      }
    }
  }

  fun visitWords(wordSeparator: Char, visitor: Visitor<V>) {
    visit(root, "", wordSeparator, visitor)
  }

  fun visit(wordSeparator: Char, visitor: Visitor<V>) {
    visit(root, "", wordSeparator, visitor)
  }

  private fun visit(node: Node<V>, prefix: String, wordSeparator: Char, visitor: Visitor<V>) {
    if (node.children.isEmpty()) {
      visitor.visit(prefix, node.value, true)
    } else {
      for ((char, child) in node.children) {
        if (char == wordSeparator) {
          visitor.visit(prefix, node.value, false)
        }
        visit(child, prefix + char, wordSeparator, visitor)
      }
    }
  }

  val length: Int
    get() {
      var size = 0
      visitWords('.') { _, _, leaf ->
        if (leaf) size++
      }
      return size
    }

  fun interface Visitor<V> {
    fun visit(word: String, value: V?, isLeaf: Boolean)
  }
}


package com.jetbrains.plugin.structure.base.utils.trie


class Trie<V>() {
  private data class Node<V>(
    val children: MutableMap<Char, Node<V>> = HashMap(),
    var value: V? = null,
    var isTerminal: Boolean = false
  )

  private fun empty() = Node<V>(HashMap(), value = null)

  private val root = empty()

  val isEmpty: Boolean get() = root.isChildless && root.value == null && !root.isTerminal
  private val Node<V>.isChildless: Boolean get() = children.isEmpty()

  fun contains(word: CharSequence): Boolean = findNode(word) != null

  fun findValue(word: CharSequence): V? = findNode(word)?.value

  private fun findNode(prefix: CharSequence): Node<V>? {
    var n = root
    for (c in prefix) {
      n = n.children[c] ?: return null
    }
    return n
  }

  fun insert(key: CharSequence, value: V? = null): Boolean {
    var isInserted = false
    var currentNode = root
    for (char in key) {
      currentNode = currentNode.children.getOrPut(char) { empty().also { isInserted = true } }
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
      result += visitor.visit(NodeVisitor.NodeVisit(prefix, node.value, isLeaf = true, node.isTerminal))
    } else {
      result += visitor.visit(NodeVisitor.NodeVisit(prefix, node.value, isLeaf = false, node.isTerminal))
      for ((char, child) in node.children) {
        with(prefix) {
          append(char)
          visit(child, prefix, result, visitor)
          deleteCharAt(lastIndex)
        }
      }
    }
  }

  fun interface NodeVisitor<V, R> {
    data class NodeVisit<V>(val word: CharSequence, val value: V?, val isLeaf: Boolean, val isTerminal: Boolean)

    fun visit(visit: NodeVisit<V>): R
  }

}




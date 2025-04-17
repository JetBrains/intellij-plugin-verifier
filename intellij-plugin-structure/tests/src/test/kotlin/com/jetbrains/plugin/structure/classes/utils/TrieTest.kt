package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.base.utils.trie.Trie
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.getInsertions
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.leafCount
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.nodeCount
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.valueCount
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.withDelimiter
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.withNonNullValues
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.withValue
import com.jetbrains.plugin.structure.jar.PathInJar
import org.junit.Assert.*
import org.junit.Test

class TrieTest {

  private fun newTrie(): Trie<Boolean> {
    return Trie<Boolean>()
  }

  @Test
  fun `all words are retrieved, nonrecursively (without middle prefixes)`() {
    val packages = newTrie()
    packages.insert("com.example.foo")
    packages.insert("com.example.bar")
    packages.insert("com.example.bar.zap")
    packages.insert("com.jetbrains.foo")
    packages.insert("com.jetbrains.cli")
    packages.insert("com.jetbrains.cli.impl")

    val expected = setOf(
      "com.example.bar.zap",
      "com.example.foo",
      "com.jetbrains.cli.impl",
      "com.jetbrains.foo"
    )
    val visitor = TrieTraversals.Leaves<Boolean>()
    packages.visit(visitor)
    assertEquals(expected, visitor.result)
  }

  @Test
  fun `all words are retrieved with explicit values`() {
    val packages = newTrie().apply {
      insert("com.example.foo", true)
      insert("com.example.bar", true)
      insert("com.example.bar.zap", true)
      insert("com.jetbrains.foo", true)
      insert("com.jetbrains.cli", true)
      insert("com.jetbrains.cli.impl", true)
    }

    val expected = setOf(
      "com.example.foo",
      "com.example.bar",
      "com.example.bar.zap",
      "com.jetbrains.foo",
      "com.jetbrains.cli",
      "com.jetbrains.cli.impl",
    )
    assertEquals(expected, packages.withValue(true))
  }

  @Test
  fun `word in trie is found, others are not`() {
    val packages = Trie<Boolean>()
    packages.insert("com.example.foo")
    packages.insert("com.example.bar")
    packages.insert("com.example.bar.zap")
    packages.insert("com.jetbrains.foo")
    packages.insert("com.jetbrains.cli")
    packages.insert("com.jetbrains.cli.impl")

    assertTrue(packages.contains("com.example"))
    assertTrue(packages.contains("com.example.bar.zap"))
    assertFalse(packages.contains("com.unavailable"))
    assertTrue(packages.contains(""))
  }

  @Test
  fun `empty trie`() {
    val emptyTrie = newTrie()
    assertTrue(emptyTrie.isEmpty)
  }

  @Test
  fun `empty trie has zero length`() {
    val emptyTrie = newTrie()
    assertEquals(0, emptyTrie.nodeCount())
  }

  @Test
  fun `trie with an empty string inserted is not empty`() {
    val emptyTrie = newTrie()
    emptyTrie.insert("")
    assertFalse(emptyTrie.isEmpty)
  }

  @Test
  fun `trie with an empty string inserted with an explicit value`() {
    val emptyTrie = newTrie()
    emptyTrie.insert("", true)
    assertFalse(emptyTrie.isEmpty)
  }

  @Test
  fun `empty trie is not traversed at all`() {
    val emptyTrie = newTrie()
    var wasVisited = false
    emptyTrie.visit { wasVisited = true }
    assertFalse(wasVisited)
  }

  @Test
  fun `retrieve full words - terminals`() {
    val packages = newTrie().apply {
      insert("com.example.foo")
      insert("com.example.foo.zap")
      insert("com.example")
    }
    var terminalCount = 0
    packages.visit { v ->
      if (v.isTerminal) terminalCount++
    }
    assertEquals(3, terminalCount)
  }

  @Test
  fun `duplicate insertions are tracked`() {
    val packages = newTrie()
    assertTrue(packages.insert("com.example.foo"))
    assertFalse(packages.insert("com.example.foo"))
    assertTrue(packages.insert("com.example.foo.zap"))
    assertFalse(packages.insert("com.example.foo.zap"))

    assertFalse(packages.insert("com.example"))
  }

  @Test
  fun `trie size is calculated`() {
    val packages = newTrie()
    packages.insert("com.example.foo")
    assertEquals(1, packages.leafCount())
    packages.insert("com.example.bar")
    assertEquals(2, packages.leafCount())
    packages.insert("com.example.bar.zap")
    assertEquals(2, packages.leafCount())
    packages.insert("com.jetbrains.foo")
    assertEquals(3, packages.leafCount())
    packages.insert("com.jetbrains.cli")
    assertEquals(4, packages.leafCount())
    packages.insert("com.jetbrains.cli.impl")
    assertEquals(4, packages.leafCount())
  }

  @Test
  fun `word with specific value is found`() {
    val packages = Trie<String>()
    packages.insert("com.example.foo", "FOO")
    packages.insert("com.example.bar", "BAR")
    packages.insert("com.example.bar.zap", "ZAP")

    assertEquals("FOO", packages.findValue("com.example.foo"))
    assertEquals("BAR", packages.findValue("com.example.bar"))
    assertEquals("ZAP", packages.findValue("com.example.bar.zap"))

    assertEquals(null, packages.findValue("com.unavailable"))
  }


  @Test
  fun `all dot-separated components are collected`() {
    val packages = newTrie().apply {
      insert("com.example.foo")
      insert("com.example.bar")
      insert("com.example.bar.zap")
      insert("com.jetbrains.foo")
      insert("com.jetbrains.cli")
      insert("com.jetbrains.cli.impl")
    }

    val results = packages.withDelimiter('.')

    assertEquals(
      setOf(
        "com",
        "com.example",
        "com.example.foo",
        "com.example.bar",
        "com.example.bar.zap",
        "com.jetbrains",
        "com.jetbrains.foo",
        "com.jetbrains.cli",
        "com.jetbrains.cli.impl"
      ),
      results
    )
  }

  @Test
  fun `delimiter collection in a trie with a single empty word`() {
    val packages = newTrie().apply {
      insert("")
    }
    assertEquals(setOf(""), packages.withDelimiter('.'))
  }

  @Test
  fun `nested classes are matched with short package`() {
    val aClass = "a/A"
    val bClass = "a/A\$B"
    val classNames = listOf(aClass, bClass)
    val trie = Trie<PathInJar>()
    classNames.forEach { trie.insert(it) }
    // There are 2 strings, one is a prefix of the other. There is only one leaf.
    assertEquals(1, trie.leafCount())
    // The node count corresponds to the length of the longer string plus 1 for the root node with an empty prefix
    assertEquals(bClass.length + 1, trie.nodeCount())
  }

  @Test
  fun `long list of class names inserted into a trie is matched`() {
    val lines = listOf(
      "bundle/IdeBundle",

      "com/intellij/execution/filters/Filter",
      "com/intellij/execution/filters/Filter\$Result",
      "com/intellij/execution/filters/Filter\$ResultItem",

      "defaults/Iface",
      "defaults/IfaceDefault",

      "experimental/ExperimentalApiEnclosingClass",
      "experimental/ExperimentalApiEnclosingClass\$NestedClass",

      "inheritance/A",
      "inheritance/AImpl",
      "inheritance/AImpl$1",
    )
    // leaves represent groups that share a common prefix.
    // Each leaf has the longest prefix
    val leafs = listOf(
      "bundle/IdeBundle",
      "com/intellij/execution/filters/Filter\$ResultItem",
      "defaults/IfaceDefault",
      "experimental/ExperimentalApiEnclosingClass\$NestedClass",
      "inheritance/AImpl$1",
    )

    val trie = Trie<Boolean>()
    lines.forEach { trie.insert(it, true) }

    assertEquals(leafs.size, trie.leafCount())
    // Each leaf has its length. No leaf has a common prefix with another note-
    // Hence there are as many nodes as sum of lengths of leaf prefixes plus one for the root node.
    assertEquals(leafs.sumOf { it.length } + 1, trie.nodeCount())

    assertEquals(lines.size, trie.valueCount(true))
  }

  @Test
  fun `traverse non null values`() {
    val trie = Trie<Boolean>()
    trie.insert("com", true)
    trie.insert("com.example.foo", true)

    val expected = setOf("com", "com.example.foo")
    assertEquals(expected, trie.withNonNullValues())
  }

  @Test
  fun `traverse explicitly assigned values`() {
    val trie = Trie<String>()
    trie.insert("com", "no")
    trie.insert("com.example.foo", "yes")
    trie.insert("org", "no")
    trie.insert("org.example", "yes")

    val expected = setOf("com.example.foo", "org.example")
    assertEquals(expected, trie.withValue("yes"))
  }

  @Test
  fun `traverse inserted words`() {
    val trie = Trie<String>().apply {
      insert("com")
      insert("com.example.foo")
      insert("org")
      insert("org.example")
    }

    val expected = setOf("com", "com.example.foo", "org", "org.example")
    assertEquals(expected, trie.getInsertions())
  }
}
package com.jetbrains.plugin.structure.classes.utils

import com.jetbrains.plugin.structure.classes.utils.TrieTraversals.collect
import org.junit.Assert.*
import org.junit.Test

class TrieTest {

  private fun newTrie(): Trie<Boolean> {
    return Trie<Boolean>(defaultValue = false)
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
    packages.visit('.', visitor)
    assertEquals(expected, visitor.result)
  }

  @Test
  fun `all words are retrieved, recursively`() {
    val packages = newTrie()
    packages.insert("com.example.foo")
    packages.insert("com.example.bar")
    packages.insert("com.example.bar.zap")
    packages.insert("com.jetbrains.foo")
    packages.insert("com.jetbrains.cli")
    packages.insert("com.jetbrains.cli.impl")

    val expected = setOf(
      "com",
      "com.example",
      "com.example.foo",
      "com.example.bar",
      "com.example.bar.zap",
      "com.jetbrains",
      "com.jetbrains.foo",
      "com.jetbrains.cli",
      "com.jetbrains.cli.impl",
    )
    val visitor = TrieTraversals.All<Boolean>()
    packages.visit('.', visitor)
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
    val visitor = TrieTraversals.WithValue<Boolean>(true)
    packages.visit('.', visitor)
    assertEquals(expected, visitor.result)
  }

  @Test
  fun `all dot separated words are retrieved`() {
    val packages = newTrie()
    packages.insert("com.example.foo")
    packages.insert("com.example.bar")
    packages.insert("com.example.bar.zap")
    packages.insert("com.jetbrains.foo")
    packages.insert("com.jetbrains.cli")
    packages.insert("com.jetbrains.cli.impl")

    val expected = setOf(
      "com", "com.example", "com.example.foo", "com.example.bar", "com.example.bar.zap",
      "com.jetbrains", "com.jetbrains.foo",
      "com.jetbrains.cli", "com.jetbrains.cli.impl"
    )
    val actualWords = packages.collect('.', TrieTraversals.All<Boolean>())
    assertEquals(expected, actualWords)
  }

  @Test
  fun `word in trie is found, others are not`() {
    val packages = Trie<Boolean>(defaultValue = false)
    packages.insert("com.example.foo")
    packages.insert("com.example.bar")
    packages.insert("com.example.bar.zap")
    packages.insert("com.jetbrains.foo")
    packages.insert("com.jetbrains.cli")
    packages.insert("com.jetbrains.cli.impl")

    assertTrue(packages.find("com.example"))
    assertTrue(packages.find("com.example.bar.zap"))
    assertFalse(packages.find("com.unavailable"))
    assertTrue(packages.find(""))
  }

  @Test
  fun `empty trie`() {
    val emptyTrie = newTrie()
    assertTrue(emptyTrie.isEmpty)
  }

  @Test
  fun `empty trie has zero length`() {
    val emptyTrie = newTrie()
    assertEquals(0, emptyTrie.length)
  }

  @Test
  fun `trie with an empty string inserted`() {
    val emptyTrie = newTrie()
    emptyTrie.insert("")
    assertTrue(emptyTrie.isEmpty)
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
    emptyTrie.visit('.') { _, _, _ ->
      wasVisited = true
    }
    assertFalse(wasVisited)
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
    assertEquals(1, packages.length)
    packages.insert("com.example.bar")
    assertEquals(2, packages.length)
    packages.insert("com.example.bar.zap")
    assertEquals(2, packages.length)
    packages.insert("com.jetbrains.foo")
    assertEquals(3, packages.length)
    packages.insert("com.jetbrains.cli")
    assertEquals(4, packages.length)
    packages.insert("com.jetbrains.cli.impl")
    assertEquals(4, packages.length)
  }

  @Test
  fun `word with specific value is found`() {
    val packages = Trie<String>(defaultValue = "")
    packages.insert("com.example.foo", "FOO")
    packages.insert("com.example.bar", "BAR")
    packages.insert("com.example.bar.zap", "ZAP")

    assertEquals("FOO", packages.findValue("com.example.foo"))
    assertEquals("BAR", packages.findValue("com.example.bar"))
    assertEquals("ZAP", packages.findValue("com.example.bar.zap"))

    assertEquals(null, packages.findValue("com.unavailable"))
  }

}
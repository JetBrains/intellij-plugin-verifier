package com.jetbrains.plugin.structure.classes.utils

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
    val words = packages.findAllWords(true)
    assertEquals(expected, words)
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
    val visitedWords = mutableSetOf<String>()
    packages.visitWords('.') { word,_,  _ ->
      visitedWords += word
    }
    assertEquals(expected, visitedWords)
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
}
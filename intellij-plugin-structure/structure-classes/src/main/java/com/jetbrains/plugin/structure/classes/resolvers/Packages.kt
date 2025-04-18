package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.utils.Trie
import com.jetbrains.plugin.structure.classes.utils.TrieTraversals.getInsertions
import com.jetbrains.plugin.structure.classes.utils.TrieTraversals.withDelimiter

typealias BinaryPackageName = String

private const val ROOT_PACKAGE_NAME = ""

class Packages {
  private val trie = Trie<Boolean>()

  fun addClass(binaryClassName: CharSequence) {
    val pkg = binaryClassName.lastIndexOf('/')
      .takeIf { it != -1 }
      ?.let { binaryClassName.subSequence(0, it)  }
      ?: ""

    addPackage(pkg)
  }

  fun addPackage(binaryPackageName: CharSequence) {
    trie.insert(binaryPackageName)
  }

  operator fun contains(packageName: BinaryPackageName): Boolean = trie.contains(packageName)

  val entries: Set<BinaryPackageName>
    get() = trie.getInsertions()

  val all: Set<BinaryPackageName>
    get() = trie.withDelimiter('/')
}
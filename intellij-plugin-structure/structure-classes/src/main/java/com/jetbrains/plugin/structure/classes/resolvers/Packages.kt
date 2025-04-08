package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.utils.Trie
import com.jetbrains.plugin.structure.classes.utils.TrieTraversals

typealias BinaryPackageName = String

private const val TRACK_MIDDLE_PACKAGES = true

private const val ROOT_PACKAGE_NAME = ""

class Packages {
  private val trie = Trie<Boolean>(false)

  fun addClass(binaryClassName: CharSequence) {
    val pkg = binaryClassName.lastIndexOf('/')
      .takeIf { it != -1 }
      ?.let { binaryClassName.subSequence(0, it)  }
      ?: ""

    trie.insert(pkg, TRACK_MIDDLE_PACKAGES)
  }

  operator fun contains(packageName: BinaryPackageName): Boolean = trie.find(packageName)

  val entries: Set<BinaryPackageName>
    get() {
      val visitor = TrieTraversals.WithValue(TRACK_MIDDLE_PACKAGES)
      trie.visit(wordSeparator = '/', visitor)
      return visitor.result
    }

  val all: Set<BinaryPackageName>
    get() {
      val visitor = TrieTraversals.All<Boolean>()
      trie.visit(wordSeparator = '/', visitor)
      return visitor.result - ROOT_PACKAGE_NAME
    }
}
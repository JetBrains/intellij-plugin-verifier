package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.utils.Trie

typealias BinaryPackageName = String

private const val TRACK_MIDDLE_PACKAGES = true

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
    get() = trie.findAllWords(value = TRACK_MIDDLE_PACKAGES, wordSeparator = '/')

  val all: Set<BinaryPackageName>
    get() = mutableSetOf<BinaryPackageName>().also { allPackageNames ->
      if(!trie.isEmpty) {
        trie.visitWords('/') { packageName: BinaryPackageName, _, _ ->
          allPackageNames += packageName
        }
      }
    }
}
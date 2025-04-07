package com.jetbrains.plugin.structure.classes.resolvers

import com.jetbrains.plugin.structure.classes.utils.Trie

typealias BinaryPackageName = String

class Packages {
  private val trie = Trie<Boolean>()

  fun addClass(binaryClassName: CharSequence) {
    val pkg = binaryClassName.lastIndexOf('/')
      .takeIf { it != -1 }
      ?.let { binaryClassName.subSequence(0, it)  }
      ?: ""

    trie.insert(pkg)
  }

  operator fun contains(packageName: BinaryPackageName): Boolean = trie.find(packageName)

  val all: Set<BinaryPackageName>
    get() = mutableSetOf<BinaryPackageName>().also { allPackageNames ->
      if(!trie.isEmpty) {
        trie.visitWords('/') { packageName: BinaryPackageName, _, _ ->
          allPackageNames += packageName
        }
      }
    }
}
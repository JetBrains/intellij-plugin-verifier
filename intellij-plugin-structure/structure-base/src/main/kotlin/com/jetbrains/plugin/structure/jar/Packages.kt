/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.trie.Trie
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.getInsertions
import com.jetbrains.plugin.structure.base.utils.trie.TrieTraversals.getAllNodes

typealias BinaryPackageName = String

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
    get() = trie.getAllNodes()
}
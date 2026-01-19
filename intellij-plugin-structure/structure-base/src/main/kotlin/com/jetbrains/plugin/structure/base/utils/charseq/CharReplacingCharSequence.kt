/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.charseq

import java.nio.CharBuffer

class CharReplacingCharSequence(private val characters: CharSequence, private val oldChar: Char, private val replacement: Char) :
  SpecialCharSequence() {
  override val length: Int
    get() = characters.length

  override fun get(index: Int): Char {
    val c = characters[index]
    if (oldChar == replacement) return c
    return if (c == oldChar) replacement else c
  }

  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
    val subSequence = characters.subSequence(startIndex, endIndex)
    if (oldChar == replacement) {
      return subSequence
    }
    return CharReplacingCharSequence(subSequence, oldChar, replacement)
  }

  override fun toString(): String {
    if (oldChar == replacement) return characters.toString()

    val newBuf = CharBuffer.allocate(length)
    for (i in indices) {
      newBuf.put(i, get(i))
    }
    return newBuf.toString()
  }
}
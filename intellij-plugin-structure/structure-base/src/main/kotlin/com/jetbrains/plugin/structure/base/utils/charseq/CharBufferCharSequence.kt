/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.charseq

import java.nio.CharBuffer

class CharBufferCharSequence(private val buffer: CharBuffer, private val startIndex: Int, private val endIndex: Int) :
  SpecialCharSequence() {

  init {
    if (startIndex < 0 || endIndex > buffer.length || startIndex > endIndex) {
      throw IndexOutOfBoundsException("Invalid start or end index: $startIndex, $endIndex")
    }
  }

  override val length: Int
    get() = endIndex - startIndex

  override fun get(index: Int): Char {
    if (index !in indices) {
      throw IndexOutOfBoundsException("Index out of bounds: $index")
    }
    return buffer.get(startIndex + index)
  }

  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
    if (startIndex < 0 || endIndex > length || startIndex > endIndex) {
      throw IndexOutOfBoundsException("Invalid subSequence range: $startIndex, $endIndex")
    }
    return CharBufferCharSequence(buffer, this.startIndex + startIndex, this.startIndex + endIndex)
  }

  override fun toString(): String {
    return buffer.subSequence(startIndex, endIndex).toString()
  }
}
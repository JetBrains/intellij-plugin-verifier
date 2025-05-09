/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.charseq

import java.nio.CharBuffer

class CharBufferCharSequence(private val buffer: CharBuffer, private val startIndex: Int, private val endIndex: Int) :
  CharSequence {

  init {
    if (startIndex < 0 || endIndex > buffer.length || startIndex > endIndex) {
      throw IndexOutOfBoundsException("Invalid start or end index")
    }
  }

  override val length: Int
    get() = endIndex - startIndex

  override fun get(index: Int): Char {
    if (index < 0 || index >= length) {
      throw IndexOutOfBoundsException("Index out of bounds: " + index)
    }
    return buffer.get(startIndex + index)
  }

  override fun subSequence(subStart: Int, subEnd: Int): CharSequence {
    if (subStart < 0 || subEnd > length || subStart > subEnd) {
      throw IndexOutOfBoundsException("Invalid subSequence range")
    }
    return CharBufferCharSequence(buffer, startIndex + subStart, startIndex + subEnd)
  }

  override fun toString(): String {
    return buffer.subSequence(startIndex, endIndex).toString()
  }
}
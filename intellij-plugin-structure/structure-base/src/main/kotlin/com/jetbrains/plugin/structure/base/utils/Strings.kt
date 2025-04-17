/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import java.nio.CharBuffer

class CharReplacingCharSequence(private val characters: CharSequence, private val oldChar: Char, private val replacement: Char) :
  CharSequence {
  override val length: Int
    get() = characters.length

  override fun get(index: Int): Char {
    val c = characters[index]
    return if (c == oldChar) replacement else c
  }

  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
    return CharReplacingCharSequence(characters.subSequence(startIndex, endIndex), oldChar, replacement)
  }

  override fun toString(): String {
    val newBuf = CharBuffer.allocate(characters.length)
    for (i in 0..characters.length - 1) {
      newBuf.put(i, get(i))
    }
    return newBuf.toString()
  }
}

fun CharSequence.occurrences(c: Char): Int {
  var count = 0
  for (i in 0..length - 1) {
    if (this[i] == c) {
      count++
    }
  }
  return count
}

fun CharSequence.componentAt(index: Int, separator: Char): String? {
  if (index < 0) return null

  var start = 0
  var currentComponent = 0
  for (i in indices) {
    if (this[i] == separator) {
      if (currentComponent == index) {
        return subSequence(start, i).toString()
      }
      currentComponent++
      start = i + 1
    }
  }

  return if (currentComponent == index) {
    subSequence(start, length).toString()
  } else {
    null
  }
}
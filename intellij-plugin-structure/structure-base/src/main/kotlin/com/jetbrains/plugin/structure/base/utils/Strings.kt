/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils


class ConcatenatedCharSequence(
  private val first: CharSequence,
  private val second: CharSequence
) : CharSequence {

  override val length: Int
    get() = first.length + second.length

  override fun get(index: Int): Char {
    return when {
      index < 0 || index >= length -> throw IndexOutOfBoundsException("Index: $index, Length: $length")
      index < first.length -> first[index]
      else -> second[index - first.length]
    }
  }

  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
    if (startIndex < 0 || endIndex > length || startIndex > endIndex) {
      throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex, Length: $length")
    }

    val firstLen = first.length

    return when {
      endIndex <= firstLen -> first.subSequence(startIndex, endIndex)
      startIndex >= firstLen -> second.subSequence(startIndex - firstLen, endIndex - firstLen)
      else -> {
        val firstPart = first.subSequence(startIndex, firstLen)
        val secondPart = second.subSequence(0, endIndex - firstLen)
        ConcatenatedCharSequence(firstPart, secondPart)
      }
    }
  }

  override fun toString(): String {
    return first.toString() + second.toString()
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
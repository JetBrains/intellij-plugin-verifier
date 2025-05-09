/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.utils.charseq.CharReplacingCharSequence

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

fun CharSequence.replaceCharacter(character: Char, replacement: Char, suffixToRemove: CharSequence): CharSequence {
  val noPrefix = if (get(0) == replacement) {
    subSequence(1, length)
  } else {
    this
  }
  val neitherPrefixNoSuffix = if (suffixToRemove.isNotEmpty() && noPrefix.endsWith(suffixToRemove)) {
    noPrefix.subSequence(0, noPrefix.length - suffixToRemove.length)
  } else {
    noPrefix
  }
  return if (character == replacement) {
    neitherPrefixNoSuffix
  } else {
    CharReplacingCharSequence(neitherPrefixNoSuffix, character, replacement)
  }
}
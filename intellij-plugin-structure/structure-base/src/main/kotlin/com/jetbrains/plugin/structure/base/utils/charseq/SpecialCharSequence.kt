/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils.charseq

abstract class SpecialCharSequence : CharSequence {
  private var hash = 0 // Default to 0

  override fun hashCode(): Int {
    var h: Int = hash
    if (h == 0 && isNotEmpty()) {
      h = computeHashCode()
      hash = h
    }
    return h
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other is String) {
      return equalsString(other)
    }
    if (other is CharSequence) {
      return equalsCharSequence(other)
    }
    return false
  }

  protected open fun computeHashCode(): Int {
    return stringHashCode(this, 0, length)
  }

  protected open fun equalsCharSequence(other: CharSequence): Boolean {
    if (length != other.length) {
      return false
    }
    for (i in indices) {
      if (this[i] != other[i]) {
        return false
      }
    }
    return true
  }

  protected open fun equalsString(other: String): Boolean = equalsCharSequence(other) // OR toString() == other

  companion object {
    fun stringHashCode(chars: CharSequence, from: Int, to: Int): Int {
      var h = 0
      for (off in from until to) {
        h = 31 * h + chars[off].toInt()
      }
      return h
    }

  }
}
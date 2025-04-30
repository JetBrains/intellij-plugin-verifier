/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import java.nio.CharBuffer
import kotlin.math.min

object CharSequenceComparator : Comparator<CharSequence> {
  override fun compare(cs1: CharSequence, cs2: CharSequence): Int {
    if (cs1 === cs2) return 0

    if (cs1 is CharBuffer && cs2 is CharBuffer) {
      return cs1.compareTo(cs2)
    }

    if (cs1 is String && cs2 is String) {
      return cs1.compareTo(cs2)
    }

    val len1 = cs1.length
    val len2 = cs2.length
    val shorterLen = min(len1, len2)

    for (i in 0 until shorterLen) {
      val comparison = cs1[i].compareTo(cs2[i])
      if (comparison != 0) {
        return comparison
      }
    }
    return len1 - len2
  }
}
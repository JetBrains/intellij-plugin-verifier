package com.jetbrains.plugin.structure.ide

import java.lang.Integer.min

fun getCommonsPrefix(strings: List<String>): String {
  return when (strings.size) {
    0 -> ""
    1 -> strings.first()
    else -> {
      val shortestLength = strings.minOf { it.length }
      val longestLength = strings.maxOf { it.length }
      var firstDiff = longestLength
      string@ for (index in 0 until shortestLength) {
        val c = strings[0][index]
        for (s in strings.drop(1)) {
          if (s[index] != c) {
            firstDiff = index
            break@string
          }
        }
      }
      return strings[0].substring(0, min(shortestLength, firstDiff))
    }
  }

}



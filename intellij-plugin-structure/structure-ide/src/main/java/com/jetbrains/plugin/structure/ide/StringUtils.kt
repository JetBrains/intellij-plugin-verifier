package com.jetbrains.plugin.structure.ide

import java.lang.Integer.min
import java.nio.file.Path

fun getCommonParentDirectory(paths: List<Path>): Path? {
  return when (paths.size) {
    0 -> null
    1 -> paths.first()
    else -> {
      val shortestLength = paths.minOf { it.nameCount }
      val longestLength = paths.maxOf { it.nameCount }
      var firstDiff = longestLength

      val firstPath = paths.first()
      index@ for (index in 0 until shortestLength) {
        val c = firstPath[index]
        for (p in paths.drop(1)) {
          if (p[index] != c) {
            firstDiff = index
            break@index
          }
        }
      }
      firstPath.prefixSubpath(min(shortestLength, firstDiff))
    }
  }
}

private fun Path.prefixSubpath(endIndex: Int): Path? {
  if (endIndex == 0) return null
  return this.subpath(0, endIndex)
}

private operator fun Path.get(index: Int) = getName(index)



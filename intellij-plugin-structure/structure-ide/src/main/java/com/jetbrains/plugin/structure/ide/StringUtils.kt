package com.jetbrains.plugin.structure.ide

import com.jetbrains.plugin.structure.intellij.platform.Layout
import java.lang.Integer.min
import java.nio.file.Path

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

fun commonParentDirectory(classpathable: Layout.Classpathable): Path? {
  val classpath = classpathable.getClasspath()
  return when (classpath.size) {
    0 -> null
    1 -> classpath.first()
    else -> {
      val prefix = getCommonsPrefix(classpath.map { it.toString() })
      if (prefix.isEmpty()) {
        null
      } else {
        Path.of(prefix)
      }
    }
  }
}



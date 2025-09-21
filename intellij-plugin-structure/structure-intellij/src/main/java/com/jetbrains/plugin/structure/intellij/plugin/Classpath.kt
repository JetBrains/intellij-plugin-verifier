package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.ClasspathOrigin.IMPLICIT
import java.nio.file.Path

class Classpath private constructor(val entries: List<ClasspathEntry> = emptyList<ClasspathEntry>()) {
  companion object {
    @JvmField
    val EMPTY = Classpath()

    fun of(paths: Collection<Path>, origin: ClasspathOrigin = IMPLICIT): Classpath {
      val entries = paths.map { ClasspathEntry(it, origin) }
      return Classpath(entries)
    }
  }

  val size: Int = entries.size

  val paths: Set<Path> = entries.map { it.path }.toSet()

  fun getUnique(): Classpath {
    return if (size == 1) {
      this
    } else {
      Classpath(entries.distinctBy { it.path.toString() })
    }
  }

  fun mergeWith(other: Classpath): Classpath {
    if (other.entries.isEmpty()) return this
    if (this.entries.isEmpty()) return other

    val pathToEntry = mutableMapOf<Path, ClasspathEntry>()

    for (cpEntry in this.entries + other.entries) {
      val path = cpEntry.path
      val existingCpEntry = pathToEntry[path]
      if (existingCpEntry == null) {
        pathToEntry[path] = cpEntry
      } else if (existingCpEntry.origin == IMPLICIT && cpEntry.origin != IMPLICIT) {
        pathToEntry[path] = cpEntry
      }
    }

    return Classpath(pathToEntry.values.toList())
  }


  override fun toString(): String = entries.joinToString(separator = ":", prefix = "[", postfix = "]")
}

class ClasspathEntry(val path: Path, val origin: ClasspathOrigin = IMPLICIT) {
  override fun toString() = "$path ($origin)"
}

enum class ClasspathOrigin {
  IMPLICIT,

  /**
   * Declared in `product-info.json`
   */
  PRODUCT_INFO,

  /**
   * Available in the artifact - usually in the 'lib' directory
   */
  PLUGIN_ARTIFACT
}
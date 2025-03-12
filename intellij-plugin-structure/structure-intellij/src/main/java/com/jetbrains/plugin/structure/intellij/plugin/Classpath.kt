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
  PRODUCT_INFO
}
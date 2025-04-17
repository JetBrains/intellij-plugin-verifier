package com.jetbrains.plugin.structure.classes.resolvers.jar

import com.jetbrains.plugin.structure.classes.utils.CharReplacingCharSequence
import java.io.File
import java.util.zip.ZipEntry

interface JarEntryResolver<T> {
  val key: Key<T>

  fun resolve(path: PathInJar, zipEntry: ZipEntry): T?

  data class Key<T>(val name: String, val type: Class<T>)
}

internal fun resolve(path: PathInJar, separator: Char, suffixToRemove: CharSequence): CharSequence {
  val noPrefix = if (path.get(0) == separator) {
    path.subSequence(1, path.length)
  } else {
    path
  }
  val neitherPrefixNoSuffix = if (suffixToRemove.isNotEmpty() && noPrefix.endsWith(suffixToRemove)) {
    noPrefix.subSequence(0, noPrefix.length - suffixToRemove.length)
  } else {
    noPrefix
  }
  return if (separator == File.separatorChar) {
    neitherPrefixNoSuffix
  } else {
    CharReplacingCharSequence(neitherPrefixNoSuffix, File.separatorChar, separator)
  }
}
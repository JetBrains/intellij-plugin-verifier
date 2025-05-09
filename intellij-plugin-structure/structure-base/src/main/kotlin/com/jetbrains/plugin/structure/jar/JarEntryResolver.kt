package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.utils.charseq.CharReplacingCharSequence
import java.util.zip.ZipEntry

interface JarEntryResolver<T> {
  val key: Key<T>

  fun resolve(path: PathInJar, zipEntry: ZipEntry): T?

  data class Key<T>(val name: String, val type: Class<T>)
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
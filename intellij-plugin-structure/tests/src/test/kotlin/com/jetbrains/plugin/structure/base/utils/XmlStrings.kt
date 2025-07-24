/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.problems.ALLOWED_NAME_SYMBOLS
import kotlin.random.Random

private val xml10BMPCharRanges = listOf(
  0x9.range(),
  0xA.range(),
  0xD.range(),
  0x20..0xD7FF,
  0xE000..0xFFFD
)

/**
 * Returns random XML character for XML 1.0 specification, but from BMP plane only.
 *
 * Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
 *
 * See [XML 1.0 Specification](https://www.w3.org/TR/xml/#charsets).
 */
internal fun getRandomXmlChar(): Char {
  val range = xml10BMPCharRanges.random()
  val codePoint = Random.nextInt(range.first, range.last + 1)

  return codePoint.toChar()
}

/**
 * Get a random plugin name composed solely of invalid plugin name characters
 * for plugins based on the XML format.
 */
internal fun getRandomInvalidXmlBasedPluginName(length: Int): String {
  val invalidPluginName = StringBuilder()
  while (true) {
    val randomXmlCharacter = getRandomXmlChar()
    if (!ALLOWED_NAME_SYMBOLS.matches(randomXmlCharacter.toString())) {
      invalidPluginName.append(randomXmlCharacter)
      if (invalidPluginName.length == length) {
        return invalidPluginName.toString()
      }
    }
  }
}

/**
 * Normalize new-lines according to [XML 1.0 Specification](https://www.w3.org/TR/xml/#sec-line-ends)
 */
internal fun String.normalizeNewLines(): String = replace("\r\n", "\n").replace("\r", "\n")

private fun Int.range() = IntRange(this, this)
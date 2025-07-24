/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.utils

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
 * @see https://www.w3.org/TR/xml/#charsets
 */
internal fun getRandomXmlChar(): Char {
  val range = xml10BMPCharRanges.random()
  val codePoint = Random.nextInt(range.first, range.last + 1)

  return codePoint.toChar()
}

private fun Int.range() = IntRange(this, this)
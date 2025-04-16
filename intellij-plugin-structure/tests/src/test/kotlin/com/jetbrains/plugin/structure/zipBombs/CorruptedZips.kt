package com.jetbrains.plugin.structure.zipBombs

import com.jetbrains.plugin.structure.base.utils.createZipBufferWithSingleEmptyFile

internal fun getZipWithoutEocd(): ByteArray? {
  val zipBytes = createZipBufferWithSingleEmptyFile()
  // EOCD signature: 0x06054b50 -> bytes: 50 4B 05 06
  val eocdSignature = byteArrayOf(0x50.toByte(), 0x4B.toByte(), 0x05.toByte(), 0x06.toByte())
  val lastEOCDIndex = zipBytes.lastIndexOf(eocdSignature)
  if (lastEOCDIndex == -1) {
    return null
  }
  val corruptedBytes = zipBytes.copyOfRange(0, lastEOCDIndex)
  return corruptedBytes
}

private fun ByteArray.lastIndexOf(pattern: ByteArray): Int {
  outer@ for (i in size - pattern.size downTo 0) {
    for (j in pattern.indices) {
      if (this[i + j] != pattern[j]) continue@outer
    }
    return i
  }
  return -1
}

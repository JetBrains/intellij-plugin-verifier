package com.jetbrains.plugin.structure.base.utils

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun createZipBufferWithSingleEmptyFile(): ByteArray {
  val byteOut = ByteArrayOutputStream()
  ZipOutputStream(byteOut).use { zipOut ->
    val entry = ZipEntry("empty.bin")
    zipOut.putNextEntry(entry)
    // No data written to the file
    zipOut.closeEntry()
  }
  return byteOut.toByteArray()
}

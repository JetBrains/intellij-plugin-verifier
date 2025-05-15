package com.jetbrains.plugin.structure.base.utils

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val TAR_GZ_CONTENT_FILE_NAME = "a"
private const val TAR_GZ_CONTENT_FILE_SIZE = 0

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

/**
 * Creates a single-file TAR.GZ.
 *
 * The file inside this archive will have a single-character name and no content (0 bytes).
 */
internal fun createMinimalisticTarGz(tarGzPath: Path) {
  val fileContent = ByteArray(TAR_GZ_CONTENT_FILE_SIZE)
  tarGzPath.outputStream().use { tarGzPathStream ->
    GzipCompressorOutputStream(tarGzPathStream).use { gzipOut ->
      TarArchiveOutputStream(gzipOut).use { tarOut ->
        val entry = TarArchiveEntry(TAR_GZ_CONTENT_FILE_NAME)
        entry.size = TAR_GZ_CONTENT_FILE_SIZE.toLong()

        tarOut.putArchiveEntry(entry)
        tarOut.write(fileContent)
        tarOut.closeArchiveEntry()
      }
    }
  }
}
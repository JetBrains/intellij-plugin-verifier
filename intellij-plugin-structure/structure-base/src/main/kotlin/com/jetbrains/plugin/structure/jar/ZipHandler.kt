package com.jetbrains.plugin.structure.jar

import java.io.File
import java.io.InputStream
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

interface ZipHandler<Z: ZipResource> {
  fun <T> iterate(handler: (ZipEntry, Z) -> T?): List<T>

  fun <T> handleEntry(entryName: CharSequence, handler: (Z, ZipEntry) -> T?): T?
}

fun Path.newZipHandler(): ZipHandler<out ZipResource> {
  return if (supportsFile()) {
    ZipFileHandler(toFile())
  } else {
    ZipInputStreamHandler(this)
  }
}

class ZipFileHandler(private val zipFile: File) : ZipHandler<ZipResource.ZipFileResource> {
  override fun <T> iterate(handler: (ZipEntry, ZipResource.ZipFileResource) -> T?): List<T> {
    val results = mutableListOf<T>()
    ZipFile(zipFile).use { zip ->
      val entries = zip.entries()
      val zipResource = ZipResource.ZipFileResource(zip)
      while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        handler(entry, zipResource)?.let { results += it }
      }
    }
    return results
  }

  override fun <T> handleEntry(entryName: CharSequence, handler: (ZipResource.ZipFileResource, ZipEntry) -> T?): T? {
    return ZipFile(zipFile).use { zip ->
      val entry: ZipEntry? = zip.getEntry(entryName.toString())
      if (entry != null) {
        val zipResource = ZipResource.ZipFileResource(zip)
        handler(zipResource, entry)
      } else {
        null
      }
    }
  }
}

class ZipInputStreamHandler(private val zipPath: Path) : ZipHandler<ZipResource.ZipStreamResource> {
  override fun <T> iterate(handler: (ZipEntry, ZipResource.ZipStreamResource) -> T?): List<T> {
    val results = mutableListOf<T>()
    Files.newInputStream(zipPath).use {
      ZipInputStream(it).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
          val resource = ZipResource.ZipStreamResource(zip)
          handler(entry, resource)?.let { results += it }

          zip.closeEntry()
          entry = zip.nextEntry
        }
      }
    }
    return results
  }

  override fun <T> handleEntry(entryName: CharSequence, handler: (ZipResource.ZipStreamResource, ZipEntry) -> T?): T? {
    return Files.newInputStream(zipPath).use {
      ZipInputStream(it).use { zip ->
        var entry: ZipEntry? = zip.nextEntry
        while (entry != null) {
          val resource = ZipResource.ZipStreamResource(zip)
          try {
            if (entry.name.contentEquals(entryName)) {
              return handler(resource, entry)
            }
          } finally {
            zip.closeEntry()
          }
          entry = zip.nextEntry
        }
        null
      }
    }
  }
}

sealed class ZipResource {
  abstract fun getInputStream(zipEntry: ZipEntry): InputStream

  data class ZipFileResource(val zipFile: ZipFile) : ZipResource() {
    override fun getInputStream(zipEntry: ZipEntry): InputStream {
      return zipFile.getInputStream(zipEntry)
    }
  }

  data class ZipStreamResource(val zipStream: ZipInputStream) : ZipResource() {
    override fun getInputStream(zipEntry: ZipEntry): InputStream {
      return zipStream
    }
  }
}

private fun Path.supportsFile() = fileSystem == FileSystems.getDefault()
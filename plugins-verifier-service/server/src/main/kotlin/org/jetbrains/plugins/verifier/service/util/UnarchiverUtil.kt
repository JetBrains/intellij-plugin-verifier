package org.jetbrains.plugins.verifier.service.util

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.AbstractUnArchiver
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver
import org.codehaus.plexus.archiver.zip.ZipUnArchiver
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

private val LOG = LoggerFactory.getLogger("UnarchiveLogger")

fun File.extractTo(destination: File): File {
  if (!this.isFile) {
    throw IllegalArgumentException("The file $this is not an archive")
  }
  if (!name.endsWith(".zip") && !name.endsWith(".tar.gz") && !name.endsWith(".tar.bz2")) {
    throw IllegalArgumentException("Unsupported archive type of $this")
  }

  try {
    FileUtils.forceMkdir(destination)
    val archiver = createUnArchiver(this)
    archiver.enableLogging(ConsoleLogger(Logger.LEVEL_WARN, "Unarchive logger"))
    archiver.destDirectory = destination
    archiver.extract()

    stripTopLevelDirectory(destination)
  } catch(e: IOException) {
    val message = "Unable to extract $this to $destination"
    LOG.error(message, e)
    throw IOException(message, e)
  }

  return destination
}

private fun createUnArchiver(file: File): AbstractUnArchiver {
  val name = file.name.toLowerCase()
  when {
    name.endsWith(".tar.gz") -> return TarGZipUnArchiver(file)
    name.endsWith(".tar.bz2") -> return TarBZip2UnArchiver(file)
    name.endsWith(".zip") -> return ZipUnArchiver(file)
    else -> throw RuntimeException("Unable to extract - unknown file extension: " + name)
  }
}

@Throws(IOException::class)
private fun stripTopLevelDirectory(dir: File) {
  val entries = dir.list()
  if (entries == null || entries.size != 1) {
    return
  }

  val singleFile = File(dir, entries[0])

  if (!singleFile.isDirectory || singleFile.list() == null) {
    return
  }

  for (entry in singleFile.list()!!) {
    if (entry == singleFile.name) {
      continue
    }

    val from = File(singleFile, entry)
    val to = File(dir, entry)
    Files.move(from, to)
  }

  FileUtils.deleteQuietly(singleFile)
}
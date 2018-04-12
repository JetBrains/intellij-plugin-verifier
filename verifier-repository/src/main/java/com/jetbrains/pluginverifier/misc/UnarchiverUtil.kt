package com.jetbrains.pluginverifier.misc

import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.AbstractUnArchiver
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver
import org.codehaus.plexus.archiver.zip.ZipUnArchiver
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import java.io.File
import java.io.IOException

fun File.extractTo(destination: File): File {
  if (!isFile) {
    throw IllegalArgumentException("The file $this is not an archive")
  }
  if (!name.endsWith(".zip") && !name.endsWith(".tar.gz") && !name.endsWith(".tar.bz2")) {
    throw IllegalArgumentException("Unsupported archive type of $this")
  }

  try {
    FileUtils.forceMkdir(destination)
    val unArchiver = createUnarchiver(this)
    unArchiver.enableLogging(ConsoleLogger(Logger.LEVEL_WARN, "Unarchive logger"))
    unArchiver.destDirectory = destination
    unArchiver.extract()
  } catch (e: Exception) {
    destination.deleteLogged()
    throw IOException("Unable to extract $this to $destination", e)
  } catch (e: Throwable) {
    destination.deleteLogged()
    throw e
  }

  return destination
}

private fun createUnarchiver(file: File): AbstractUnArchiver {
  val name = file.name.toLowerCase()
  return when {
    name.endsWith(".tar.gz") -> TarGZipUnArchiver(file)
    name.endsWith(".tar.bz2") -> TarBZip2UnArchiver(file)
    name.endsWith(".zip") -> ZipUnArchiver(file)
    else -> throw RuntimeException("Unable to extract - unknown file extension: $name")
  }
}
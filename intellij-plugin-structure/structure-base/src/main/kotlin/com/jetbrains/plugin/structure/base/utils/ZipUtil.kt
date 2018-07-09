package com.jetbrains.plugin.structure.base.utils

import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.AbstractArchiver
import org.codehaus.plexus.archiver.AbstractUnArchiver
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.codehaus.plexus.archiver.tar.TarArchiver
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver
import org.codehaus.plexus.archiver.util.DefaultFileSet
import org.codehaus.plexus.archiver.zip.ZipArchiver
import org.codehaus.plexus.archiver.zip.ZipUnArchiver
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import java.io.File
import java.io.IOException

@Throws(IOException::class)
fun archiveDirectory(directory: File, destination: File) {
  val archiver = createArchiver(destination)
  archiver.enableLogging(ConsoleLogger(Logger.LEVEL_ERROR, "Unarchive logger"))
  archiver.addFileSet(DefaultFileSet.fileSet(directory).prefixed(directory.name + "/"))
  archiver.destFile = destination
  archiver.createArchive()
}

private fun createArchiver(destination: File): AbstractArchiver {
  val name = destination.name.toLowerCase()
  return when {
    name.endsWith(".tar.gz") -> TarArchiver()
    name.endsWith(".zip") -> ZipArchiver()
    name.endsWith(".jar") -> JarArchiver()
    else -> throw IllegalArgumentException("Unable to extract $destination. Unknown file extension: $name")
  }
}

fun File.extractTo(destination: File): File {
  if (!isFile) {
    throw IllegalArgumentException("The file $this is not an archive")
  }
  if (!name.endsWith(".zip") && !name.endsWith(".tar.gz") && !name.endsWith(".tar.bz2")) {
    throw IllegalArgumentException("Unsupported archive type: $this")
  }
  FileUtils.forceMkdir(destination)
  val unArchiver = createUnArchiver(this)
  unArchiver.enableLogging(ConsoleLogger(Logger.LEVEL_WARN, ""))
  unArchiver.destDirectory = destination
  unArchiver.extract()
  return destination
}

private fun createUnArchiver(archive: File): AbstractUnArchiver {
  val name = archive.name.toLowerCase()
  return when {
    name.endsWith(".tar.gz") -> TarGZipUnArchiver(archive)
    name.endsWith(".tar.bz2") -> TarBZip2UnArchiver(archive)
    name.endsWith(".zip") -> ZipUnArchiver(archive)
    else -> throw RuntimeException("Unable to extract - unknown file extension: $name")
  }
}
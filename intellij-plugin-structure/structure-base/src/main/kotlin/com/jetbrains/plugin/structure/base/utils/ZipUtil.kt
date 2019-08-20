package com.jetbrains.plugin.structure.base.utils

import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.AbstractUnArchiver
import org.codehaus.plexus.archiver.ArchiverException
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.codehaus.plexus.archiver.tar.TarArchiver
import org.codehaus.plexus.archiver.tar.TarBZip2UnArchiver
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver
import org.codehaus.plexus.archiver.util.DefaultFileSet
import org.codehaus.plexus.archiver.xz.XZUnArchiver
import org.codehaus.plexus.archiver.zip.ZipArchiver
import org.codehaus.plexus.archiver.zip.ZipUnArchiver
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Archives [directory] to [destination].
 *
 * Extension of [destination] specifies an archiver to use,
 * which can be one of `.tar.gz`, `.zip`, or `.jar`.
 *
 * If [includeDirectory] is `true`, the directory itself
 * will be included to the archive, otherwise only
 * directory's content will be.
 *
 * If [includeEmptyDirectories] is `true`, empty source
 * directories will be included to the archive, otherwise
 * they will be skipped.
 */
@Throws(IOException::class)
fun archiveDirectory(
    directory: File,
    destination: File,
    includeDirectory: Boolean = true,
    includeEmptyDirectories: Boolean = true
) {
  destination.deleteLogged()
  FileUtils.forceMkdirParent(destination.absoluteFile)
  val archiver = createArchiver(destination.extension).apply {
    enableLogging(ConsoleLogger(Logger.LEVEL_ERROR, "Unarchive logger"))
    addFileSet(
        DefaultFileSet
            .fileSet(directory)
            .prefixed(if (includeDirectory) directory.name + "/" else "")
            .includeEmptyDirs(includeEmptyDirectories)
    )
    destFile = destination.absoluteFile
  }
  archiver.createArchive()
}

fun InputStream.xzInputStream(): InputStream = XZUnArchiver.getXZInputStream(this)

private fun createArchiver(extension: String) =
    when (extension) {
      "tar.gz" -> TarArchiver()
      "zip" -> ZipArchiver()
      "jar" -> JarArchiver()
      else -> throw IllegalArgumentException("Unknown file extension: $extension")
    }

class ArchiveSizeLimitExceededException(val sizeLimit: Long): RuntimeException()

fun File.extractTo(destination: File, outputSizeLimit: Long? = null): File {
  require(isFile) { "The file $this is not an archive" }
  require(name.endsWith(".zip") || name.endsWith(".tar.gz") || name.endsWith(".tar.bz2")) {
    "Unsupported archive type: $this"
  }
  FileUtils.forceMkdir(destination)
  val unArchiver = createUnArchiver(this, outputSizeLimit)
  unArchiver.enableLogging(ConsoleLogger(Logger.LEVEL_WARN, ""))
  unArchiver.destDirectory = destination
  checkIfInterrupted()
  try {
    unArchiver.extract()
  } catch (e: Exception) {
    if (outputSizeLimit != null && e is ArchiverException && e.message?.let { it.contains("size", true) && it.contains("limit", true) } == true) {
      throw ArchiveSizeLimitExceededException(outputSizeLimit)
    }
    e.rethrowIfInterrupted()
    throw e
  }
  return destination
}

private fun createUnArchiver(archive: File, outputSizeLimit: Long?): AbstractUnArchiver {
  val name = archive.name.toLowerCase()
  return when {
    name.endsWith(".tar.gz") -> TarGZipUnArchiver(archive)
    name.endsWith(".tar.bz2") -> TarBZip2UnArchiver(archive)
    name.endsWith(".zip") -> ZipUnArchiver(archive).apply {
      if (outputSizeLimit != null) {
        setMaxOutputSize(outputSizeLimit)
      }
    }
    else -> throw RuntimeException("Unable to extract - unknown file extension: $name")
  }
}

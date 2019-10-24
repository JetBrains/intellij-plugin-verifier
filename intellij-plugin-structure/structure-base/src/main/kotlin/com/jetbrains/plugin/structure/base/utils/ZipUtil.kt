package com.jetbrains.plugin.structure.base.utils

import com.jetbrains.plugin.structure.base.decompress.TarDecompressor
import com.jetbrains.plugin.structure.base.decompress.ZipDecompressor
import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.codehaus.plexus.archiver.tar.TarArchiver
import org.codehaus.plexus.archiver.util.DefaultFileSet
import org.codehaus.plexus.archiver.xz.XZUnArchiver
import org.codehaus.plexus.archiver.zip.ZipArchiver
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
  val name = destination.name
  val builder = when {
    name.endsWith(".tar.gz") -> TarArchiver()
    name.endsWith(".zip") -> ZipArchiver()
    name.endsWith(".jar") -> JarArchiver()
    else -> throw IllegalArgumentException("Unknown file extension: $name")
  }

  builder.enableLogging(ConsoleLogger(Logger.LEVEL_ERROR, "Archive logger"))
  builder.addFileSet(
    DefaultFileSet
      .fileSet(directory)
      .prefixed(if (includeDirectory) directory.name + "/" else "")
      .includeEmptyDirs(includeEmptyDirectories)
  )

  builder.destFile = destination.absoluteFile
  builder.createArchive()
}

fun InputStream.xzInputStream(): InputStream = XZUnArchiver.getXZInputStream(this)

fun File.extractTo(destination: File, outputSizeLimit: Long? = null): File {
  val decompressor = when {
    name.endsWith(".zip") -> ZipDecompressor(this, outputSizeLimit)
    name.endsWith(".tar.gz") -> TarDecompressor(this, outputSizeLimit)
    else -> throw IllegalArgumentException("Unknown type archive type: ${destination.name}")
  }

  destination.createDir()
  decompressor.extract(destination)
  return destination
}
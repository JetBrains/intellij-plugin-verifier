package com.jetbrains.plugin.structure.base.utils

import com.google.common.base.Throwables
import org.apache.commons.io.FileUtils
import org.codehaus.plexus.archiver.AbstractArchiver
import org.codehaus.plexus.archiver.jar.JarArchiver
import org.codehaus.plexus.archiver.tar.TarArchiver
import org.codehaus.plexus.archiver.zip.ZipArchiver
import org.codehaus.plexus.archiver.zip.ZipUnArchiver
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
object ZipUtil {
  @Throws(IOException::class)
  fun extractZip(pluginZip: File, destDir: File) {
    try {
      val ua = ZipUnArchiver(pluginZip)
      ua.enableLogging(ConsoleLogger(Logger.LEVEL_WARN, ""))
      ua.destDirectory = destDir
      ua.extract()
    } catch (e: Throwable) {
      FileUtils.deleteQuietly(destDir)
      throw Throwables.propagate(e)
    }
  }

  fun <T> withExtractedZipArchive(pluginZip: File, callback: (File) -> T): T {
    val destinationDirectory = FileUtil.createTempDir(FileUtil.extractedPluginsDirectory, "plugin_")
    extractZip(pluginZip, destinationDirectory)
    try {
      val result = callback(destinationDirectory)
      FileUtils.deleteDirectory(destinationDirectory)
      return result
    } catch (e: Throwable) {
      FileUtils.deleteDirectory(destinationDirectory)
      throw e
    }
  }

  @Throws(IOException::class)
  fun archiveDirectory(directory: File, destination: File) {
    val archiver = createArchiver(destination)
    archiver.enableLogging(ConsoleLogger(Logger.LEVEL_ERROR, "Unarchive logger"))
    archiver.addDirectory(directory, directory.name + "/")
    archiver.destFile = destination
    archiver.createArchive()
  }

  private fun createArchiver(file: File): AbstractArchiver {
    val name = file.name.toLowerCase()

    if (name.endsWith(".tar.gz")) {
      return TarArchiver()
    } else if (name.endsWith(".zip")) {
      return ZipArchiver()
    } else if (name.endsWith(".jar")) {
      return JarArchiver()
    }
    throw IllegalArgumentException("Unable to extract $file- unknown file extension: $name")
  }
}

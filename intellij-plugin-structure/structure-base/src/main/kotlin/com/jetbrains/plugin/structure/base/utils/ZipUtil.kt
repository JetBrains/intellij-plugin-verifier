package com.jetbrains.plugin.structure.base.utils

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

object ZipUtil {
  @Throws(IOException::class)
  fun extractZip(pluginZip: File, destDir: File) {
    val ua = ZipUnArchiver(pluginZip)
    ua.enableLogging(ConsoleLogger(Logger.LEVEL_WARN, ""))
    FileUtils.forceMkdir(destDir)
    ua.destDirectory = destDir
    ua.extract()
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

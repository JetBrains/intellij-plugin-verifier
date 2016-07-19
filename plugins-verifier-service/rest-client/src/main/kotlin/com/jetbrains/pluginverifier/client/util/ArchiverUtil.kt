package com.jetbrains.pluginverifier.client.util

import org.codehaus.plexus.archiver.zip.ZipArchiver
import java.io.File

/**
 * @author Sergey Patrikeev
 */
object ArchiverUtil {
  fun archiveDirectory(dir: File, destFile: File): File {
    val archiver = ZipArchiver()
    archiver.destFile = destFile
    archiver.addDirectory(dir)
    archiver.createArchive()
    return destFile
  }

}
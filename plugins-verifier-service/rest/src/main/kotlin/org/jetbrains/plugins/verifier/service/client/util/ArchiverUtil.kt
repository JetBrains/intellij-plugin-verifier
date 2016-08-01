package org.jetbrains.plugins.verifier.service.client.util

import org.codehaus.plexus.archiver.zip.ZipArchiver
import java.io.File

/**
 * @author Sergey Patrikeev
 */
object ArchiverUtil {
  fun archiveDirectory(dir: File, destFile: File) {
    val archiver = ZipArchiver()
    archiver.destFile = destFile
    archiver.addDirectory(dir)
    archiver.createArchive()
  }

}
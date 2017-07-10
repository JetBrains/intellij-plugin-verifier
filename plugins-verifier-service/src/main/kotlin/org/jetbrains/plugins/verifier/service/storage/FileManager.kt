package org.jetbrains.plugins.verifier.service.storage

import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.setting.Settings
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * @author Sergey Patrikeev
 */
object FileManager {

  fun createTempDirectory(dirName: String): File = Files.createTempDirectory(getTempDirectory().toPath(), dirName).toFile()

  fun createTempFile(suffix: String): File = Files.createTempFile(getTempDirectory().toPath(), "", suffix).toFile()

  private fun dirName(type: FileType): String = when (type) {
    FileType.IDE -> "ides"
    FileType.PLUGIN -> "plugins"
  }

  fun getTypeDir(type: FileType): File = findOrCreateDirectory(File(getAppHomeDirectory(), dirName(type)))

  fun getAppHomeDirectory(): File = findOrCreateDirectory(Settings.APP_HOME_DIRECTORY.get())

  private fun findOrCreateDirectory(path: String): File = findOrCreateDirectory(File(path))

  private fun findOrCreateDirectory(res: File): File {
    if (!res.isDirectory) {
      FileUtils.forceMkdir(res)
      if (!res.isDirectory) {
        throw IOException("Failed to create directory: " + res)
      }
    }
    return res
  }


  fun getTempDirectory(): File = findOrCreateDirectory(File(getAppHomeDirectory(), "temp"))

  fun getFilesOfType(fileType: FileType): List<File> = getTypeDir(fileType).listFiles().toList()

  fun getFileByName(name: String, fileType: FileType): File = File(getTypeDir(fileType), name)

}


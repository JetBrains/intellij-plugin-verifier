package org.jetbrains.plugins.verifier.service.storage

import com.jetbrains.pluginverifier.misc.createDir
import java.io.File
import java.nio.file.Files

/**
 * @author Sergey Patrikeev
 */
class FileManager(private val homeDir: File) {

  fun createTempDirectory(dirName: String): File = Files.createTempDirectory(getTempDirectory().toPath(), dirName).toFile()

  fun createTempFile(suffix: String): File = Files.createTempFile(getTempDirectory().toPath(), "", suffix).toFile()

  private fun dirName(type: FileType): String = when (type) {
    FileType.IDE -> "ides"
    FileType.PLUGIN -> "plugins"
  }

  fun getTypeDir(type: FileType): File = homeDir.resolve(dirName(type)).createDir()

  fun getTempDirectory(): File = homeDir.resolve("temp").createDir()

  fun getFilesOfType(fileType: FileType): List<File> = getTypeDir(fileType).listFiles().toList()

  fun getFileByName(name: String, fileType: FileType): File = File(getTypeDir(fileType), name)

}


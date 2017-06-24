package org.jetbrains.plugins.verifier.service.storage

import com.jetbrains.pluginverifier.misc.deleteLogged
import org.apache.commons.io.FileUtils
import org.jetbrains.plugins.verifier.service.setting.Settings
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files

private val LOG = LoggerFactory.getLogger(FileManager::class.java)

/**
 * @author Sergey Patrikeev
 */
object FileManager : IFileManager {

  @Synchronized
  override fun createTempDirectory(dirName: String): File = Files.createTempDirectory(getTempDirectory().toPath(), dirName).toFile()

  @Synchronized
  override fun createTempFile(suffix: String): File = Files.createTempFile(getTempDirectory().toPath(), "", suffix).toFile()

  private fun dirName(type: FileType): String = when (type) {
    FileType.IDE -> "ides"
    FileType.PLUGIN -> "plugins"
    FileType.REPORT -> "reports"
  }

  @Synchronized
  override fun getTypeDir(type: FileType): File = findOrCreateDirectory(File(getAppHomeDirectory(), dirName(type)))

  @Synchronized
  override fun getAppHomeDirectory(): File = findOrCreateDirectory(Settings.APP_HOME_DIRECTORY.get())

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


  @Synchronized
  override fun save(source: File, fileType: FileType, overwrite: Boolean, removeSource: Boolean) {
    source.copyTo(getFileByName(source.name, fileType), overwrite)
    if (removeSource) {
      source.deleteLogged()
    }
  }

  @Synchronized
  override fun delete(fileName: String, fileType: FileType) {
    val file = getFileByName(fileName, fileType)
    if (file.exists()) {
      file.deleteLogged()
    } else {
      LOG.warn("Request of the non-existent file: $file")
    }
  }

  @Synchronized
  override fun getTempDirectory(): File = findOrCreateDirectory(File(getAppHomeDirectory(), "temp"))

  @Synchronized
  override fun getFilesOfType(fileType: FileType): List<File> = getTypeDir(fileType).listFiles().toList()

  @Synchronized
  override fun getFileByName(name: String, fileType: FileType): File = File(getTypeDir(fileType), name)


}

/**
 * @author Sergey Patrikeev
 */
interface IFileManager {

  fun save(source: File, fileType: FileType, overwrite: Boolean = false, removeSource: Boolean = false)

  fun delete(fileName: String, fileType: FileType)

  fun getAppHomeDirectory(): File

  fun getTempDirectory(): File

  fun getFilesOfType(fileType: FileType): List<File>

  fun getFileByName(name: String, fileType: FileType): File?

  fun getTypeDir(type: FileType): File

  fun createTempFile(suffix: String): File

  fun createTempDirectory(dirName: String): File

}

enum class FileType {
  IDE,
  PLUGIN,
  REPORT;
}
package com.jetbrains.plugin.structure.base.utils

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException

/**
 * @author Sergey Patrikeev
 */
object FileUtil {

  private val TEMP_DIR_ATTEMPTS = 10000

  private val tempDirectory: File
    get() {
      val tempDir = System.getProperty("intellij.structure.temp.dir")
      if (tempDir != null) {
        return File(tempDir)
      }
      return FileUtils.getTempDirectory()
    }

  val extractedPluginsDirectory: File
    @Throws(IOException::class)
    get() {
      val dir = File(tempDirectory, "extracted-plugins")
      if (!dir.isDirectory) {
        try {
          if (dir.exists()) {
            FileUtils.forceDelete(dir)
          }
          FileUtils.forceMkdir(dir)
        } catch (e: IOException) {
          throw IOException("Unable to create plugins cache directory " + dir.absoluteFile + " (check access permissions)", e)
        }

      }
      return dir
    }

  //it's synchronized because otherwise there is a possibility of two threads creating the same directory
  @Synchronized
  @Throws(IOException::class)
  fun createTempDir(parent: File, prefix: String): File {
    val baseName = prefix + "_" + System.currentTimeMillis()
    var lastException: IOException? = null
    for (counter in 0..TEMP_DIR_ATTEMPTS - 1) {
      val tempDir = File(parent, baseName + "_" + counter)
      if (!tempDir.exists()) {
        try {
          FileUtils.forceMkdir(tempDir)
          return tempDir
        } catch (ioe: IOException) {
          lastException = ioe
        }

      }
    }
    throw IllegalStateException("Failed to create directory under " + parent.absolutePath + " within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "_0 to " + baseName + "_" + (TEMP_DIR_ATTEMPTS - 1) + ')', lastException)
  }

  private fun hasExtension(file: File, extension: String): Boolean =
      file.isFile && extension == Files.getFileExtension(file.name)

  fun isJarOrZip(file: File): Boolean = isJar(file) || isZip(file)

  fun isZip(file: File): Boolean = hasExtension(file, "zip")

  fun isJar(file: File): Boolean = hasExtension(file, "jar")
}

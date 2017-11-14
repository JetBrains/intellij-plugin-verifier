package com.jetbrains.pluginverifier.storage

import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.deleteLogged
import java.io.File
import java.nio.file.Files

/**
 * todo: rename?
 * @author Sergey Patrikeev
 */
class FileManager(private val directory: File) {

  private val tempDirectory = directory.resolve("temp").createDir()

  fun createTempDirectory(name: String): File = Files.createTempDirectory(tempDirectory.toPath(), name).toFile()

  fun createTempFile(suffix: String): File = Files.createTempFile(tempDirectory.toPath(), "temp", suffix).toFile()

  fun cleanupTempDirectories() = tempDirectory.deleteLogged()

}


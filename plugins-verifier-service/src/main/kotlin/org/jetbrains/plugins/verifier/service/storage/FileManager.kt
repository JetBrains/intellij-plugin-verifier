package org.jetbrains.plugins.verifier.service.storage

import com.jetbrains.pluginverifier.misc.createDir
import com.jetbrains.pluginverifier.misc.deleteLogged
import java.io.File
import java.nio.file.Files

/**
 * @author Sergey Patrikeev
 */
class FileManager(applicationHomeDirectory: File) {

  private val tempDirectoryRoot = applicationHomeDirectory.resolve("temp").createDir().toPath()

  fun createTempDirectory(name: String): File = Files.createTempDirectory(tempDirectoryRoot, name).toFile()

  fun createTempFile(suffix: String): File = Files.createTempFile(tempDirectoryRoot, "temp", suffix).toFile()

  fun cleanupTempDirectories() = tempDirectoryRoot.toFile().deleteLogged()

}


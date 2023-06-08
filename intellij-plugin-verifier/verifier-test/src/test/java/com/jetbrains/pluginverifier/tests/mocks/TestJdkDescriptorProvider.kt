package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import java.lang.IllegalArgumentException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

object TestJdkDescriptorProvider {
  private const val JVM_HOME_DIRS = "/usr/lib/jvm"

  var filesystem: FileSystem = FileSystems.getDefault()

  fun getJdkPathForTests(): Path {
    val candidates = mutableListOf<Path?>()
    candidates.add(System.getenv("JAVA_HOME")?.let { filesystem.getPath(it) })
    candidates.add(System.getProperty("user.home")?.let {
      filesystem.getPath(it, ".sdkman/candidates/java/current")
    })
    candidates.add(fromJvmHomeDirs())
    candidates.add(System.getProperty("java.home")?.let { filesystem.getPath(it) })

    return candidates.filterNotNull()
      .firstOrNull(Path::exists)
      .also {
        println("Using $it as JDK in tests")
      }
      ?: throw IllegalArgumentException("No suitable JDK is found for the test. " +
        "Set the JAVA_HOME environment variable, " +
        "or verify the 'java.home' property, " +
        "or setup Java via SDKMan or install the JDK to the $JVM_HOME_DIRS directory")
  }

  private fun fromJvmHomeDirs(): Path? {
    val jvmHomeDir = filesystem.getPath(JVM_HOME_DIRS)
    return when {
      jvmHomeDir.exists() -> {
        jvmHomeDir.listFiles().firstOrNull { it.isDirectory }
      }
      else -> null
    }
  }
}
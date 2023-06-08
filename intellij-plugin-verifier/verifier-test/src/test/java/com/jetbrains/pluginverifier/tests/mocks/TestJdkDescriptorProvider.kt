package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path

const val PV_TESTJAVA_HOME_PROPERTY_NAME = "pluginverifier.testjava.home"

object TestJdkDescriptorProvider {
  private val LOG: Logger = LoggerFactory.getLogger(TestJdkDescriptorProvider::class.java)

  private const val JVM_HOME_DIRS = "/usr/lib/jvm"

  var filesystem: FileSystem = FileSystems.getDefault()

  fun getJdkPathForTests(): Path {
    val candidates = listOf(
      fromTestJavaHomeProperty(),
      fromJavaHome(),
      fromSdkMan(),
      fromJvmHomeDirs(),
      fromJavaHomeProperty()
    )

    return candidates.filterNotNull()
      .firstOrNull(Path::exists)
      .also {
        LOG.info("Using $it as JDK in tests")
      }
      ?: throw IllegalArgumentException("No suitable JDK is found for the test. " +
        "Set the JAVA_HOME environment variable, " +
        "or verify the 'java.home' property, " +
        "or setup Java via SDKMan or install the JDK to the $JVM_HOME_DIRS directory")
  }

  private fun fromSdkMan(): Path? {
    return System.getProperty("user.home")?.let {
      filesystem.getPath(it, ".sdkman/candidates/java/current")
    }
  }

  private fun fromJavaHome()
    = System.getenv("JAVA_HOME")?.let { filesystem.getPath(it) }

  private fun fromTestJavaHomeProperty() =
    System.getProperty(PV_TESTJAVA_HOME_PROPERTY_NAME)?.let { filesystem.getPath(it) }

  private fun fromJavaHomeProperty()
  = System.getProperty("java.home")?.let { filesystem.getPath(it) }

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
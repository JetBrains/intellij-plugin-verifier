package com.jetbrains.pluginverifier.parameters.jdk

import com.jetbrains.pluginverifier.misc.isDirectory
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Path to a JDK that will be used for the verification.
 */
data class JdkPath(val jdkPath: Path) {
  override fun toString() = jdkPath.toString()

  companion object {
    fun createJdkPath(jdkPath: String): JdkPath {
      val jdkDir = Paths.get(jdkPath)
      require(jdkDir.isDirectory) { "Specified invalid JDK path: $jdkPath" }
      return JdkPath(jdkDir)
    }

    fun createJavaHomeJdkPath(): JdkPath {
      val javaHome = System.getenv("JAVA_HOME")
      requireNotNull(javaHome) { "JAVA_HOME is not specified" }
      val jdkDir = Paths.get(javaHome)
      require(jdkDir.isDirectory) { "JAVA_HOME points to invalid JDK: $javaHome" }
      return JdkPath(jdkDir)
    }
  }
}
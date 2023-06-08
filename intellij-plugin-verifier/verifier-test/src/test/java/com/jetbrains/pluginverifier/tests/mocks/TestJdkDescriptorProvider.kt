package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import java.lang.IllegalArgumentException
import java.nio.file.Path
import java.nio.file.Paths

object TestJdkDescriptorProvider {
  private const val JVM_HOME_DIRS = "/usr/lib/jvm"

  fun getJdkPathForTests(): Path {
    val javaHomeFromEnv = System.getenv("JAVA_HOME")?.let { Paths.get(it) }
    if (javaHomeFromEnv != null && javaHomeFromEnv.exists()) {
      return javaHomeFromEnv
    }

    val javaHomeFromProperty = System.getProperty("java.home")?.let { Paths.get(it) }
    if (javaHomeFromProperty != null && javaHomeFromProperty.exists()) {
      return javaHomeFromProperty
    }

    val javaHomeFromSdkMan = System.getProperty("user.home")?.let {
      Paths.get(it, ".sdkman/candidates/java/current")
    }

    if (javaHomeFromSdkMan != null && javaHomeFromSdkMan.exists()) {
      return javaHomeFromSdkMan
    }

    val jvmHomeDir = Paths.get(JVM_HOME_DIRS)
    if (jvmHomeDir.exists()) {
      val someJdk = jvmHomeDir.listFiles().firstOrNull { it.isDirectory }
      if (someJdk != null) {
        println("Using $someJdk as JDK in tests")
        return someJdk
      }
    }

    throw IllegalArgumentException("No suitable JDK is found for the test. Set the JAVA_HOME environment variable or install the JDK to the $JVM_HOME_DIRS directory")
  }
}
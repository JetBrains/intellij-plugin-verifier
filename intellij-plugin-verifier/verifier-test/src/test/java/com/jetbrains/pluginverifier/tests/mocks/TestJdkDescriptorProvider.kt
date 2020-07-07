package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import java.lang.IllegalArgumentException
import java.nio.file.Path
import java.nio.file.Paths

object TestJdkDescriptorProvider {

  fun getJdkPathForTests(): Path {
    val javaHome = System.getenv("JAVA_HOME")?.let { Paths.get(it) }
    if (javaHome != null && javaHome.exists()) {
      return javaHome
    }

    val jvmHomeDir = Paths.get("/usr/lib/jvm")
    if (jvmHomeDir.exists()) {
      val someJdk = jvmHomeDir.listFiles().firstOrNull { it.isDirectory }
      if (someJdk != null) {
        println("Using $someJdk as JDK in tests")
        return someJdk
      }
    }

    throw IllegalArgumentException("No suitable JDK is found for the test")
  }

}
package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.misc.isDirectory
import java.nio.file.Path
import java.nio.file.Paths

object TestJdkDescriptorProvider {

  fun getJdkPathForTests(): Path {
    return Paths.get("/usr/lib/jvm/jdk-11.0.1")

    val jdk8Path = "/usr/lib/jvm/java-8-oracle"
    val jdkPath = System.getenv("JAVA_HOME") ?: jdk8Path
    val jdkDir = if ('9' in jdkPath) {
      //Todo: support the java 9
      Paths.get(jdk8Path)
    } else {
      Paths.get(jdkPath)
    }
    require(jdkDir.isDirectory)
    return jdkDir
  }

}
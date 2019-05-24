package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.parameters.jdk.JdkPath
import java.nio.file.Paths

object TestJdkDescriptorProvider {

  fun getJdkPathForTests(): JdkPath {
    val jdk8Path = "/usr/lib/jvm/java-8-oracle"
    val jdkPath = System.getenv("JAVA_HOME") ?: jdk8Path
    val jdkDir = if ('9' in jdkPath) {
      //Todo: support the java 9
      Paths.get(jdk8Path)
    } else {
      Paths.get(jdkPath)
    }
    require(jdkDir.isDirectory)
    return JdkPath(jdkDir)
  }

}
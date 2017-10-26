package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import java.io.File

/**
 * Created by Sergey.Patrikeev
 */
object TestJdkDescriptorProvider {

  fun getJdkDescriptorForTests(): JdkDescriptor {
    val jdk8Path = "/usr/lib/jvm/java-8-oracle"
    val jdkPath = System.getenv("JAVA_HOME") ?: jdk8Path
    val jdkDir = if ('9' in jdkPath) {
      //Todo: support the java 9
      File(jdk8Path)
    } else {
      File(jdkPath)
    }
    require(jdkDir.isDirectory)
    return JdkDescriptor(jdkDir)
  }

}
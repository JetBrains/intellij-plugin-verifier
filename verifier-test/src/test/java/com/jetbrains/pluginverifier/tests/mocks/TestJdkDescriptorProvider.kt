package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.classes.jdk.JdkResolverCreator
import com.jetbrains.pluginverifier.misc.isDirectory
import com.jetbrains.pluginverifier.parameters.jdk.JdkDescriptor
import java.nio.file.Paths

/**
 * Created by Sergey.Patrikeev
 */
object TestJdkDescriptorProvider {

  fun getJdkDescriptorForTests(): JdkDescriptor {
    val jdk8Path = "/usr/lib/jvm/java-8-oracle"
    val jdkPath = System.getenv("JAVA_HOME") ?: jdk8Path
    val jdkDir = if ('9' in jdkPath) {
      //Todo: support the java 9
      Paths.get(jdk8Path)
    } else {
      Paths.get(jdkPath)
    }
    require(jdkDir.isDirectory)
    return JdkDescriptor(JdkResolverCreator.createJdkResolver(jdkDir.toFile()), jdkDir)
  }

}
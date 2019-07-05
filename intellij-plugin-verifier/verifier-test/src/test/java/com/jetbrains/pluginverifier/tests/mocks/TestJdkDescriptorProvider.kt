package com.jetbrains.pluginverifier.tests.mocks

import com.jetbrains.plugin.structure.base.utils.exists
import java.nio.file.Path
import java.nio.file.Paths

object TestJdkDescriptorProvider {

  fun getJdkPathForTests(): Path {
    val jdkPath = Paths.get(System.getenv("JAVA_HOME") ?: "/usr/lib/jvm/java-8-oracle")
    check(jdkPath.exists())
    return jdkPath
  }

}
package org.jetbrains.plugins.verifier.service.service.jdks

import java.io.File

class JdkManager(private val jdk8path: File) {
  fun getJdkHome(version: JdkVersion): File = when (version) {
    JdkVersion.JAVA_8_ORACLE -> jdk8path
  }
}
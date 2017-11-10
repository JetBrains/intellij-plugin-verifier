package org.jetbrains.plugins.verifier.service.storage

import java.io.File

enum class JdkVersion {
  JAVA_8_ORACLE
}

class JdkManager(private val jdk8path: File) {

  fun getJdkHome(version: JdkVersion): File = when (version) {
    JdkVersion.JAVA_8_ORACLE -> jdk8path
  }
}
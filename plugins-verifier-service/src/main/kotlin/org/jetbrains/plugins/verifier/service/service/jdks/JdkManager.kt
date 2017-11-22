package org.jetbrains.plugins.verifier.service.service.jdks

import java.nio.file.Path

class JdkManager(private val jdk8path: Path) {
  fun getJdkHome(version: JdkVersion): Path = when (version) {
    JdkVersion.JAVA_8_ORACLE -> jdk8path
  }
}
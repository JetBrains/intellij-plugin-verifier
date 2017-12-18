package org.jetbrains.plugins.verifier.service.service.jdks

import java.nio.file.Path

/**
 * JDK manager [provides] [getJdkHome] paths to the JDKs by [versions] [JdkVersion].
 * The paths are configured on the server startup.
 */
class JdkManager(private val jdk8path: Path) {

  /**
   * Provides a path to the JDK with specified [version]
   */
  fun getJdkHome(version: JdkVersion): Path = when (version) {
    JdkVersion.JAVA_8_ORACLE -> jdk8path
  }
}
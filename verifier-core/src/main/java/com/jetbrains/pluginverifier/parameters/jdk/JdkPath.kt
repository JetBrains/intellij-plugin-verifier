package com.jetbrains.pluginverifier.parameters.jdk

import java.nio.file.Path

/**
 * Path to a JDK that will be used for the verification.
 */
data class JdkPath(val jdkPath: Path) {
  override fun toString() = jdkPath.toString()
}
package com.jetbrains.pluginverifier.parameters.jdk

import java.nio.file.Path

/**
 * @author Sergey Patrikeev
 */
data class JdkDescriptor(val homeDir: Path) {
  override fun toString(): String = homeDir.toAbsolutePath().toString()
}
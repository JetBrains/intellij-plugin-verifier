package com.jetbrains.pluginverifier.parameters.jdk

import java.io.File

/**
 * @author Sergey Patrikeev
 */
data class JdkDescriptor(val homeDir: File) {
  override fun toString(): String = homeDir.absolutePath
}
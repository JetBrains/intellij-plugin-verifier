package com.jetbrains.pluginverifier.parameters

import java.io.File

/**
 * @author Sergey Patrikeev
 */
data class JdkDescriptor(val homeDir: File) {
  override fun toString(): String = homeDir.absolutePath
}
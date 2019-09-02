package com.jetbrains.pluginverifier.jdk

import com.jetbrains.plugin.structure.intellij.version.IdeVersion

data class JdkVersion(val javaVersion: String, val bundledTo: IdeVersion?) {

  val isBundled: Boolean get() = bundledTo != null

  val majorVersion: Int = if (javaVersion.startsWith("1.")) {
    javaVersion.substringAfter("1.").substringBefore(".").toIntOrNull()
  } else {
    javaVersion.substringBefore(".").toIntOrNull()
  } ?: throw IllegalArgumentException("Invalid version: '$javaVersion'")

}
package com.jetbrains.plugin.structure.classes.jdk

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils

import java.io.File

object JdkResolverCreator {

  private val MANDATORY_JARS = setOf("rt.jar")

  private val ADDITIONAL_JARS = setOf("tools.jar", "classes.jar", "jsse.jar", "javaws.jar", "jce.jar", "jfxrt.jar", "plugin.jar")

  fun createJdkResolver(jdkPath: File): Resolver = createJdkResolver(Resolver.ReadMode.FULL, jdkPath)

  fun createJdkResolver(readMode: Resolver.ReadMode, jdkPath: File): Resolver {
    val mandatoryJars = JarsUtils.collectJars(jdkPath, { it.name.toLowerCase() in MANDATORY_JARS }, true)
    val missingJars = MANDATORY_JARS - mandatoryJars.map { it.name }
    if (missingJars.isNotEmpty()) {
      throw IllegalArgumentException("JDK ${jdkPath.absolutePath} is not a valid JDK")
    }

    val additionalJars = JarsUtils.collectJars(jdkPath, { it.name.toLowerCase() in ADDITIONAL_JARS }, true)
    return JarsUtils.makeResolver(readMode, mandatoryJars + additionalJars)
  }

}

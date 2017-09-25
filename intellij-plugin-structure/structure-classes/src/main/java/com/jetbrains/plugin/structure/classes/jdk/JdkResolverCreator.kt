package com.jetbrains.plugin.structure.classes.jdk

import com.google.common.collect.ImmutableSet
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils

import java.io.File

/**
 * @author Sergey Patrikeev
 */
object JdkResolverCreator {
  private val JDK_JAR_NAMES = ImmutableSet.of("rt.jar", "tools.jar", "classes.jar", "jsse.jar", "javaws.jar", "jce.jar", "jfxrt.jar", "plugin.jar")

  fun createJdkResolver(jdkPath: File): Resolver {
    val jars = JarsUtils.collectJars(jdkPath, { JDK_JAR_NAMES.contains(it.name.toLowerCase()) }, true)
    return JarsUtils.makeResolver(jars)
  }

}

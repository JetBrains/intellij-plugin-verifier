package com.jetbrains.pluginverifier.parameters.jdk

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.readLines
import com.jetbrains.plugin.structure.base.utils.readText
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import java.nio.file.Path

object JdkDescriptorCreator {

  fun createJdkDescriptor(jdkPath: Path): JdkDescriptor =
      createJdkDescriptor(jdkPath, Resolver.ReadMode.FULL)

  fun createJdkDescriptor(jdkPath: Path, readMode: Resolver.ReadMode): JdkDescriptor {
    val javaVersion = readJavaVersion(jdkPath)
    return if (javaVersion < 9) {
      createPreJava9(jdkPath, readMode)
    } else {
      createJava9Plus(jdkPath, readMode)
    }
  }

  private fun readJavaVersion(jdkPath: Path): Int {
    val fullVersion = findFullVersion(jdkPath)
    return parseJavaVersion(fullVersion)
  }

  private fun parseJavaVersion(fullVersion: String): Int {
    return if (fullVersion.startsWith("1.")) {
      fullVersion.substringAfter("1.").substringBefore(".").toIntOrNull()
    } else {
      fullVersion.substringBefore(".").toIntOrNull()
    } ?: throw IllegalArgumentException("Invalid version: '$fullVersion'")
  }

  private fun findFullVersion(jdkPath: Path): String {
    val releasePath = jdkPath.resolve("release")
    if (releasePath.exists()) {
      val properties = releasePath.readLines().associate {
        val list = it.split("=")
        list[0] to list[1].trim('"')
      }
      val javaVersion = properties["JAVA_VERSION"]
      checkNotNull(javaVersion) { "JAVA_VERSION is not specified in $releasePath" }
      return javaVersion
    }

    //Amazon Corretto JDK 8 has 'version.txt' instead.
    val versionPath = jdkPath.resolve("version.txt")
    if (versionPath.exists()) {
      return versionPath.readText()
    }

    throw IllegalArgumentException("JDK version is not known: neither $releasePath nor $versionPath are available")
  }

  private fun createJava9Plus(jdkPath: Path, readMode: Resolver.ReadMode): JdkDescriptor {
    val resolver = JdkJImageResolver(jdkPath, readMode)
    return JdkDescriptor(jdkPath, resolver)
  }

  private fun createPreJava9(jdkPath: Path, readMode: Resolver.ReadMode): JdkDescriptor {
    val mandatoryJars = setOf("rt.jar")
    val optionalJars = setOf("tools.jar", "classes.jar", "jsse.jar", "javaws.jar", "jce.jar", "jfxrt.jar", "plugin.jar")

    val jars = JarsUtils.collectJars(jdkPath.toFile(), {
      val name = it.name.toLowerCase()
      name in mandatoryJars || name in optionalJars
    }, true)

    val missingJars = mandatoryJars - jars.map { it.name.toLowerCase() }
    require(missingJars.isEmpty()) {
      "JDK by path does not have mandatory jars (${missingJars.joinToString()}): $jdkPath"
    }

    val jarResolver = JarsUtils.makeResolver(readMode, jars)
    return JdkDescriptor(jdkPath, jarResolver)
  }

}

package com.jetbrains.pluginverifier.parameters.jdk

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import com.jetbrains.pluginverifier.misc.exists
import com.jetbrains.pluginverifier.misc.readLines
import java.nio.file.Path

object JdkDescriptorCreator {

  fun createJdkDescriptor(jdkPath: Path): JdkDescriptor {
    val releasePath = jdkPath.resolve("release")
    require(releasePath.exists()) { "Invalid JDK: $releasePath does not exist" }

    val properties = parseProperties(releasePath)
    val javaVersion = properties["JAVA_VERSION"]
    checkNotNull(javaVersion) { "JAVA_VERSION is not specified in " }

    val majorVersion = javaVersion.substringBefore(".").toIntOrNull()
    checkNotNull(majorVersion) { "Invalid JAVA_VERSION: $javaVersion " }

    return if (majorVersion < 9) {
      createPreJava9(jdkPath)
    } else {
      createJava9Plus(jdkPath)
    }
  }

  private fun createJava9Plus(jdkPath: Path): JdkDescriptor {
    val resolver = JdkJImageResolver(jdkPath)
    return JdkDescriptor(jdkPath, resolver)
  }

  private fun createPreJava9(jdkPath: Path): JdkDescriptor {
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

    val jarResolver = JarsUtils.makeResolver(Resolver.ReadMode.FULL, jars)
    return JdkDescriptor(jdkPath, jarResolver)
  }

  private fun parseProperties(propertiesPath: Path): Map<String, String> =
      propertiesPath.readLines().associate {
        val list = it.split("=")
        list[0] to list[1].trim('"')
      }

}

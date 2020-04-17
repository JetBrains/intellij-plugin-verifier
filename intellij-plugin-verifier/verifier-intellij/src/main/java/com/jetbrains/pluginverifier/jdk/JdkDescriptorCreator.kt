/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.jdk

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.listRecursivelyAllFilesWithExtension
import com.jetbrains.plugin.structure.base.utils.readLines
import com.jetbrains.plugin.structure.base.utils.readText
import com.jetbrains.plugin.structure.classes.resolvers.CompositeResolver
import com.jetbrains.plugin.structure.classes.resolvers.JdkFileOrigin
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildJarOrZipFileResolvers
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import java.nio.file.Path

object JdkDescriptorCreator {

  fun createBundledJdkDescriptor(ide: Ide, readMode: Resolver.ReadMode = Resolver.ReadMode.FULL): JdkDescriptor? {
    val bundledJdkPath = listOf(
      ide.idePath.resolve("jbr"),
      ide.idePath.resolve("jre64")
    ).find { it.isDirectory } ?: return null
    return createJdkDescriptor(bundledJdkPath.toPath(), readMode, ide.version)
  }

  fun createJdkDescriptor(
    jdkPath: Path,
    readMode: Resolver.ReadMode = Resolver.ReadMode.FULL
  ): JdkDescriptor = createJdkDescriptor(jdkPath, readMode, null)

  private fun createJdkDescriptor(
    jdkPath: Path,
    readMode: Resolver.ReadMode,
    bundledTo: IdeVersion?
  ): JdkDescriptor {
    val fullJavaVersion = readFullVersion(jdkPath)
    val jdkVersion = JdkVersion(fullJavaVersion, bundledTo)
    return if (jdkVersion.majorVersion < 9) {
      createPreJava9(jdkPath, readMode, jdkVersion)
    } else {
      createJava9Plus(jdkPath, readMode, jdkVersion)
    }
  }

  private fun readFullVersion(jdkPath: Path): String {
    val linuxOrWindowsRelease = jdkPath.resolve("release")
    val maxOsRelease = jdkPath.resolve("Contents").resolve("Home").resolve("release")
    for (releasePath in listOf(linuxOrWindowsRelease, maxOsRelease)) {
      if (releasePath.exists()) {
        val properties = releasePath.readLines().associate {
          val list = it.split("=")
          list[0] to list[1].trim('"')
        }
        val javaVersion = properties["JAVA_VERSION"]
        checkNotNull(javaVersion) { "JAVA_VERSION is not specified in $releasePath" }
        return javaVersion
      }
    }

    //Amazon Corretto JDK 8 has 'version.txt' instead.
    val versionPath = jdkPath.resolve("version.txt")
    if (versionPath.exists()) {
      return versionPath.readText()
    }

    throw IllegalArgumentException("JDK version is not known: neither $linuxOrWindowsRelease, nor $maxOsRelease, nor $versionPath are available")
  }

  private fun createJava9Plus(jdkPath: Path, readMode: Resolver.ReadMode, jdkVersion: JdkVersion): JdkDescriptor {
    val resolver = JdkJImageResolver(jdkPath, readMode)
    return JdkDescriptor(jdkPath, resolver, jdkVersion)
  }

  private fun createPreJava9(jdkPath: Path, readMode: Resolver.ReadMode, jdkVersion: JdkVersion): JdkDescriptor {
    val mandatoryJars = setOf("rt.jar")
    val optionalJars = setOf("tools.jar", "classes.jar", "jsse.jar", "javaws.jar", "jce.jar", "jfxrt.jar", "plugin.jar")

    val jars = jdkPath.toFile().listRecursivelyAllFilesWithExtension("jar").filter { file ->
      val fileName = file.name.toLowerCase()
      fileName in mandatoryJars || fileName in optionalJars
    }

    val missingJars = mandatoryJars - jars.map { it.name.toLowerCase() }
    require(missingJars.isEmpty()) {
      "JDK $jdkPath misses mandatory jars: ${missingJars.joinToString()}"
    }

    val jarResolver = CompositeResolver.create(buildJarOrZipFileResolvers(jars, readMode, JdkFileOrigin(jdkPath)))
    return JdkDescriptor(jdkPath, jarResolver, jdkVersion)
  }

}

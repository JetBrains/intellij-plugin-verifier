/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.pluginverifier.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.jdk.JdkDescriptorCreator
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Holds IDE objects necessary for verification.
 *
 * - [ide] - instance of this IDE
 * - [ideResolver] - accessor of IDE class files
 * - [jdkDescriptor] - JDK used to run the IDE: a bundled JDK if available or a specified default JDK
 * - [ideFileLock] - a lock to protect the IDE file from deletion.
 * It will be closed along with `this` descriptor.
 */
data class IdeDescriptor(
  val ide: Ide,
  val ideResolver: Resolver,
  val jdkDescriptor: JdkDescriptor,
  val ideFileLock: FileLock?
) : Closeable {

  val ideVersion get() = ide.version

  val jdkVersion get() = jdkDescriptor.jdkVersion

  override fun toString() = ideVersion.toString()

  override fun close() {
    ideResolver.closeLogged()
    jdkDescriptor.closeLogged()
    ideFileLock.closeLogged()
  }

  companion object {
    /**
     * Creates [IdeDescriptor] for specified [idePath].
     * [ideFileLock] will be released when this [IdeDescriptor] is closed.
     */
    fun create(
      idePath: Path,
      defaultJdkPath: Path?,
      ideFileLock: FileLock?
    ): IdeDescriptor {
      val ide = IdeManager.createManager().createIde(idePath)
      val ideResolver = IdeResolverCreator.createIdeResolver(ide)
      ideResolver.closeOnException {
        val jdkDescriptor = JdkDescriptorCreator.createBundledJdkDescriptor(ide)
          ?: createDefaultJdkDescriptor(defaultJdkPath)
        return IdeDescriptor(ide, ideResolver, jdkDescriptor, ideFileLock)
      }
    }

    private fun createDefaultJdkDescriptor(defaultJdkPath: Path?): JdkDescriptor {
      val jdkPath = defaultJdkPath ?: run {
        val javaHome = System.getenv("JAVA_HOME")
        requireNotNull(javaHome) { "JAVA_HOME is not specified" }
        println("Using Java from JAVA_HOME: $javaHome")
        Paths.get(javaHome)
      }
      require(jdkPath.isDirectory) { "Invalid JDK path: $jdkPath" }
      return JdkDescriptorCreator.createJdkDescriptor(jdkPath)
    }

  }

}
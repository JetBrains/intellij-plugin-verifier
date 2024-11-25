/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.ide

import com.jetbrains.plugin.structure.base.utils.closeLogged
import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver.ReadMode
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.ide.classes.IdeResolverConfiguration
import com.jetbrains.plugin.structure.ide.classes.IdeResolverCreator
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.pluginverifier.jdk.DefaultJdkDescriptorProvider
import com.jetbrains.pluginverifier.jdk.JdkDescriptor
import com.jetbrains.pluginverifier.jdk.JdkDescriptorProvider.Result.Found
import com.jetbrains.pluginverifier.repository.files.FileLock
import java.io.Closeable
import java.nio.file.Path

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
    private val jdkDescriptorProvider = DefaultJdkDescriptorProvider()

    /**
     * Creates [IdeDescriptor] for specified [idePath].
     * [ideFileLock] will be released when this [IdeDescriptor] is closed.
     */
    fun create(
      idePath: Path,
      defaultJdkPath: Path?,
      ideFileLock: FileLock?
    ): IdeDescriptor {
      return create(idePath, defaultJdkPath, ideFileLock, MissingLayoutFileMode.SKIP_AND_WARN)
    }

    /**
     * Creates [IdeDescriptor] for specified [idePath].
     * [ideFileLock] will be released when this [IdeDescriptor] is closed.
     *
     * @param missingLayoutClasspathFileMode Behavior what to do on missing layout classpath entries.
     */
    fun create(
      idePath: Path,
      defaultJdkPath: Path?,
      ideFileLock: FileLock?,
      missingLayoutClasspathFileMode: MissingLayoutFileMode
    ): IdeDescriptor {
      val ideResolverConfiguration = IdeResolverConfiguration(ReadMode.FULL, missingLayoutClasspathFileMode)
      val ide = IdeManager.createManager().createIde(idePath)
      val ideResolver = IdeResolverCreator.createIdeResolver(ide, ideResolverConfiguration)
      ideResolver.closeOnException {
        when (val result = jdkDescriptorProvider.getJdkDescriptor(ide, defaultJdkPath)) {
          is Found -> return IdeDescriptor(ide, ideResolver, result.jdkDescriptor, ideFileLock)
          else -> throw IllegalStateException("No suitable JDK was found")
        }
      }
    }
  }

}
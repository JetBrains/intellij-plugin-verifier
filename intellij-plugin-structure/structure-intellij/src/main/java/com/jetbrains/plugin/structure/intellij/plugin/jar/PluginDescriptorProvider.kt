/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.jar

import com.jetbrains.plugin.structure.base.utils.getShortExceptionMessage
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.META_INF
import com.jetbrains.plugin.structure.jar.JarArchiveCannotBeOpenException
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Failed
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import com.jetbrains.plugin.structure.jar.PluginJar
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.inputStream
import kotlin.use

private val LOG = LoggerFactory.getLogger(PluginDescriptorProvider::class.java)

class PluginDescriptorProvider(private val fileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider) {
  fun getDescriptorFromJar(
    jarFile: Path,
    descriptorPath: String = "$META_INF/$PLUGIN_XML",
  ): PluginDescriptorResult = try {
    PluginJar(jarFile, fileSystemProvider).use { jar ->
      when (val descriptor = jar.getPluginDescriptor(descriptorPath)) {
        is Found -> descriptor.clone()
        else -> descriptor.also {
          LOG.debug("Unable to resolve descriptor [{}] from [{}] ({})", descriptorPath, jarFile, descriptor)
        }
      }
    }
  } catch (e: JarArchiveCannotBeOpenException) {
    LOG.warn("Unable to extract {} (searching for {}): {}", jarFile, descriptorPath, e.getShortExceptionMessage())
    Failed(jarFile, e)
  }

  private fun Found.clone(): Found = inputStream.use {
    Found(this.path, it.readAllBytes().inputStream())
  }
}


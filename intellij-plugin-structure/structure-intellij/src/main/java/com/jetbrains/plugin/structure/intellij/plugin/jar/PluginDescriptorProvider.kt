/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin.jar

import com.jetbrains.plugin.structure.base.utils.getShortExceptionMessage
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager.Companion.META_INF
import com.jetbrains.plugin.structure.jar.JarArchiveCannotBeOpenException
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult.Found
import com.jetbrains.plugin.structure.jar.PluginJar
import com.jetbrains.plugin.structure.jar.SingletonCachingJarFileSystemProvider
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(PluginDescriptorProvider::class.java)

private const val DEFAULT_DESCRIPTOR_PATH = "$META_INF/$PLUGIN_XML"

class PluginDescriptorProvider(private val fileSystemProvider: JarFileSystemProvider = SingletonCachingJarFileSystemProvider) {
  fun <T> resolveFromJar(jarFile: Path, onSuccess: (Found) -> T): T? {
    return try {
      PluginJar(jarFile, fileSystemProvider).use { jar ->
        when (val descriptor = jar.getPluginDescriptor(DEFAULT_DESCRIPTOR_PATH)) {
          is Found -> onSuccess(descriptor)
          else -> null.also {
            LOG.debug("Unable to resolve descriptor [{}] from [{}] ({})", DEFAULT_DESCRIPTOR_PATH, jarFile, descriptor)
          }
        }
      }
    } catch (e: JarArchiveCannotBeOpenException) {
      null.also {
        LOG.warn("Unable to extract {} (searching for {}): {}", jarFile, DEFAULT_DESCRIPTOR_PATH, e.getShortExceptionMessage())
      }
    }
  }
}


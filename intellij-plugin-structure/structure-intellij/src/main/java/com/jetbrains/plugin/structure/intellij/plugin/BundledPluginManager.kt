/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.intellij.plugin.jar.PluginDescriptorProvider
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(BundledPluginManager::class.java)

private const val PLUGINS_DIRECTORY = "plugins"
private const val LIB_DIRECTORY = "lib"

class BundledPluginManager(private val pluginIdProvider: PluginIdProvider) {
  private val descriptorProvider = PluginDescriptorProvider()

  fun getBundledPluginIds(idePath: Path): Set<PluginArtifactPath> {
    return readBundledPluginsIds(idePath)
  }

  private fun readBundledPluginsIds(
    idePath: Path
  ): Set<PluginArtifactPath> {
    return idePath
      .resolve(PLUGINS_DIRECTORY)
      .listFiles()
      .filter { it.isDirectory }
      .mapNotNull { resolveBundledPluginId(it) }
      .toSet()
  }

  private fun resolveBundledPluginId(pluginDirectory: Path): PluginArtifactPath? {
    return try {
      loadPluginIdFromDirectory(pluginDirectory)?.let {
        PluginArtifactPath(it, pluginDirectory)
      }
    } catch (e: BundledPluginException) {
      LOG.debug("Plugin [{}] has invalid descriptor: {}", pluginDirectory, e.message)
      null
    }
  }

  @Throws(BundledPluginException::class)
  private fun loadPluginIdFromDirectory(pluginPath: Path): String? {
    val jars = getPluginJars(pluginPath)
    for (jarPath in jars) {
      val pluginId = loadPluginIdFromJar(jarPath)
      if (pluginId != null) {
        return pluginId
      }
    }
    return null
  }

  @Throws(BundledPluginException::class)
  private fun loadPluginIdFromJar(jarPath: Path): String? {
    return descriptorProvider.resolveFromJar(jarPath) { (_, inputStream) ->
      pluginIdProvider.getPluginId(inputStream)
    }
  }

  @Throws(BundledPluginException::class)
  private fun getPluginJars(pluginPath: Path): List<Path> {
    val libDir: Path = pluginPath.resolve(LIB_DIRECTORY)
    if (!libDir.isDirectory) {
      throw BundledPluginException(PluginDescriptorIsNotFound(PLUGIN_XML))
    }
    val jars = libDir.listJars()
    if (jars.isEmpty()) {
      throw BundledPluginException(PluginLibDirectoryIsEmpty())
    }
    return jars
  }

  private class BundledPluginException(val problem: PluginProblem) : RuntimeException() {
    override val message: String
      get() = problem.message
  }
}
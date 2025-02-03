package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.intellij.plugin.jar.PluginDescriptorProvider
import com.jetbrains.plugin.structure.intellij.problems.PluginLibDirectoryIsEmpty
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.PluginDescriptorResult
import org.slf4j.LoggerFactory
import java.nio.file.Path

private val LOG = LoggerFactory.getLogger(BundledPluginManager::class.java)

class BundledPluginManager(private val pluginIdProvider: PluginIdProvider) {
  private val descriptorProvider = PluginDescriptorProvider()

  fun getBundledPluginIds(idePath: Path): Set<String> {
    return readBundledPluginsIds(idePath)
  }

  private fun readBundledPluginsIds(
    idePath: Path
  ): Set<String> {
    return idePath
      .resolve("plugins")
      .listFiles()
      .filter { it.isDirectory }
      .mapNotNull { resolveBundledPluginId(idePath, it) }
      .toSet()
  }

  private fun resolveBundledPluginId(idePath: Path, pluginDirectory: Path): String? {
    return try {
      loadPluginIdFromDirectory(pluginDirectory)
    } catch (e: BundledPluginException) {
      LOG.debug("Plugin [{}] has invalid descriptor: {}", pluginDirectory, e.message)
       null
    }
  }

  @Throws(BundledPluginException::class)
  private fun loadPluginIdFromDirectory(pluginPath: Path): String? {
    val libDir: Path = pluginPath.resolve("lib")
    if (!libDir.isDirectory) {
      throw BundledPluginException(PluginDescriptorIsNotFound(PLUGIN_XML))
    }
    val jars = libDir.listJars()
    if (jars.isEmpty()) {
      throw BundledPluginException(PluginLibDirectoryIsEmpty())
    }
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
    val descriptorResult = descriptorProvider.getDescriptorFromJar(jarPath)
    return if (descriptorResult is PluginDescriptorResult.Found) {
      descriptorResult.inputStream.use {
        pluginIdProvider.getPluginId(it)
      }
    } else {
      // JAR does not contain the plugin.xml, skip it.
      null
    }
  }

  private class BundledPluginException(val problem: PluginProblem) : RuntimeException()
}
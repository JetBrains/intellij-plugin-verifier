package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.plugin.IconTheme
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.plugin.parseThirdPartyDependenciesByPath
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.readBytes
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val META_INF = "META-INF"
const val PLUGIN_XML = "plugin.xml"
val PLUGIN_XML_RESOURCE_PATH = META_INF + File.separator + PLUGIN_XML

private val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

private val LOG: Logger = LoggerFactory.getLogger(PluginJar::class.java)

class PluginJar(private val jarPath: Path, private val jarFileSystemProvider: JarFileSystemProvider = DefaultJarFileSystemProvider()): AutoCloseable {

  private val jarFileSystem: FileSystem = jarFileSystemProvider.getFileSystem(jarPath).also {
    LOG.debug("Provider '{}' created file system for [{}]", jarFileSystemProvider.javaClass.name, jarPath)
  }

  fun resolveDescriptorPath(descriptorPath: String = PLUGIN_XML_RESOURCE_PATH): Path? {
    val descriptor = jarFileSystem.getPath(toCanonicalPath(descriptorPath))
    return if (Files.exists(descriptor)) {
      descriptor
    } else {
      null
    }
  }

  fun getPluginDescriptor(descriptorPathValue: String = PLUGIN_XML_RESOURCE_PATH): PluginDescriptorResult {
    val descriptorPath = resolveDescriptorPath(descriptorPathValue) ?: return PluginDescriptorResult.NotFound
    return PluginDescriptorResult.Found(descriptorPath, descriptorPath.inputStream().buffered())
  }

  fun getIcons(): List<PluginIcon> {
    val defaultIcon = findPluginIcon(IconTheme.DEFAULT)
    if (defaultIcon == null) {
      LOG.debug("Default plugin icon not found (plugin archive {})", jarPath)
      return emptyList()
    }
    return IconTheme.values().mapNotNull {
      when (it) {
        IconTheme.DEFAULT -> defaultIcon
        IconTheme.DARCULA -> findPluginIcon(it)
      }
    }
  }

  private fun findPluginIcon(theme: IconTheme): PluginIcon? {
    val iconEntryName = "$META_INF/${getIconFileName(theme)}"

    val iconPath = jarFileSystem.getPath(META_INF, getIconFileName(theme))
    return if (iconPath.exists()) {
      PluginIcon(theme, iconPath.readBytes(), iconEntryName)
    } else {
      null
    }
  }

  fun getThirdPartyDependencies(): List<ThirdPartyDependency> {
    val path = jarFileSystem.getPath(META_INF, THIRD_PARTY_LIBRARIES_FILE_NAME)
    return parseThirdPartyDependenciesByPath(path)
  }

  private fun toCanonicalPath(descriptorPath: String): String {
    return Paths.get(descriptorPath.toSystemIndependentName()).normalize().toString()
  }

  private fun getIconFileName(iconTheme: IconTheme) = "pluginIcon${iconTheme.suffix}.svg"

  override fun close() {
    jarFileSystemProvider.close(jarPath)
  }
}
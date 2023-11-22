package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.plugin.IconTheme
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.plugin.parseThirdPartyDependenciesByPath
import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.inputStream
import com.jetbrains.plugin.structure.base.utils.readBytes
import com.jetbrains.plugin.structure.base.utils.toSystemIndependentName
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val META_INF = "META-INF"
const val PLUGIN_XML = "plugin.xml"
val PLUGIN_XML_RESOURCE_PATH = META_INF + File.separator + PLUGIN_XML

private val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

class PluginJar(jarPath: Path, jarFileSystemProvider: JarFileSystemProvider = DefaultJarFileSystemProvider()) {
  private val jarFileSystem: FileSystem = jarFileSystemProvider.getFileSystem(jarPath)

  fun resolveDescriptorPath(descriptorPath: String = PLUGIN_XML_RESOURCE_PATH): Path? {
    val descriptor = jarFileSystem.getPath(toCanonicalPath(descriptorPath))
    return if (Files.exists(descriptor)) {
      descriptor
    } else {
      null
    }
  }

  fun getPluginDescriptor(descriptorPathValue: String = PLUGIN_XML_RESOURCE_PATH, charset: Charset = StandardCharsets.UTF_8): PluginDescriptorResult {
    val descriptorPath = resolveDescriptorPath(descriptorPathValue) ?: return PluginDescriptorResult.NotFound
    return PluginDescriptorResult.Found(descriptorPath, descriptorPath.inputStream().buffered())
  }

  fun getIcons(): List<PluginIcon> {
    return IconTheme.values().mapNotNull { theme ->
      val iconEntryName = "$META_INF/${getIconFileName(theme)}"

      val iconPath = jarFileSystem.getPath(META_INF, getIconFileName(theme))
      if (iconPath.exists()) {
        PluginIcon(theme, iconPath.readBytes(), iconEntryName)
      } else {
        null
      }
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
}
package com.jetbrains.plugin.structure.jar

import com.jetbrains.plugin.structure.base.plugin.IconTheme
import com.jetbrains.plugin.structure.base.plugin.PluginIcon
import com.jetbrains.plugin.structure.base.plugin.ThirdPartyDependency
import com.jetbrains.plugin.structure.base.plugin.parseThirdPartyDependenciesByPath
import com.jetbrains.plugin.structure.base.utils.*
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

const val META_INF = "META-INF"
const val PLUGIN_XML = "plugin.xml"
val PLUGIN_XML_RESOURCE_PATH = META_INF + File.separator + PLUGIN_XML
const val JAR_SCHEME = "jar"

private val THIRD_PARTY_LIBRARIES_FILE_NAME = "dependencies.json"

class PluginJar(jarPath: Path, jarFileSystemProvider: JarFileSystemProvider = DefaultJarFileSystemProvider()) : AutoCloseable {
  private val jarFileSystem: FileSystem = jarFileSystemProvider.getFileSystem(jarPath)

  fun resolveDescriptorPath(descriptorPath: String = PLUGIN_XML_RESOURCE_PATH): Path? {
    val descriptor = jarFileSystem.getPath(toCanonicalPath(descriptorPath))
    return if (Files.exists(descriptor)) {
      descriptor
    } else {
      null
    }
  }

  /**
   * Open a new [buffered reader][BufferedReader] over this JAR archive plugin descriptor.
   * The caller must handle the closing of this buffered reader.
   *
   * @return the buffered reader or `null` when the descriptor path cannot be resolved in this JAR file.
   */
  @Throws(IOException::class)
  fun openDescriptor(descriptorPath: String = PLUGIN_XML_RESOURCE_PATH, charset: Charset = StandardCharsets.UTF_8): BufferedReader? {
    return resolveDescriptorPath((descriptorPath))?.let {
      Files.newBufferedReader(it, charset)
    }
  }

  fun getPluginDescriptor(descriptorPathValue: String = PLUGIN_XML_RESOURCE_PATH, charset: Charset = StandardCharsets.UTF_8): PluginDescriptorResult {
    val descriptorPath = resolveDescriptorPath(descriptorPathValue) ?: return PluginDescriptorResult.NotFound
    return PluginDescriptorResult.Found(descriptorPath, Files.newBufferedReader(descriptorPath, charset))
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

  override fun close() {
    //FIXME close? jarFileSystem.closeLogged()
  }

  private fun toCanonicalPath(descriptorPath: String): String {
    return Paths.get(descriptorPath.toSystemIndependentName()).normalize().toString()
  }

  private fun getIconFileName(iconTheme: IconTheme) = "pluginIcon${iconTheme.suffix}.svg"
}
package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.hasExtension
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.intellij.plugin.LIB_DIRECTORY
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.createIdeaPluginXmlDetector
import com.jetbrains.plugin.structure.jar.JarArchiveCannotBeOpenException
import com.jetbrains.plugin.structure.jar.JarFileSystemProvider
import com.jetbrains.plugin.structure.jar.META_INF
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.plugin.structure.jar.PluginJar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path


private const val MODULES_DIR = "modules"

private const val XML_EXTENSION = "xml"

private val META_INF_PLUGIN_XML_PATH_COMPONENTS = listOf(META_INF, PLUGIN_XML)

private val LOG: Logger = LoggerFactory.getLogger(ContentModuleScanner::class.java)

class ContentModuleScanner(private val fileSystemProvider: JarFileSystemProvider) {
  private val ideaPluginXmlDetector = createIdeaPluginXmlDetector()

  fun getContentModules(pluginArtifact: Path) : ContentModules {
    val libDir = pluginArtifact.resolve(LIB_DIRECTORY)
    if (!libDir.exists()) {
      return ContentModules(pluginArtifact, emptyList())
    }
    val jarPaths = libDir.listJars()
    val moduleJarPaths = libDir.resolve(MODULES_DIR).listJars()
    val contentModules = (jarPaths + moduleJarPaths).flatMap { jarPath ->
      getJarContentModules(jarPath)
    }

    return ContentModules(pluginArtifact, contentModules)
  }

  private fun getJarContentModules(jarPath: Path): List<ContentModule> = withPluginJar(jarPath) { jar ->
    jar.resolveDescriptors { it.isDescriptor() }
      .map { resolveContentModule(jarPath, it) }
  } ?: emptyList()

  private fun resolveContentModule(jarPath: Path, descriptorPath: Path): ContentModule {
    val moduleName = if (descriptorPath.isMetaInfPluginXml()) {
      "ROOT"
    } else {
      descriptorPath.getModuleName()
    }
    return ContentModule(moduleName, jarPath, descriptorPath)
  }

  private fun <T> withPluginJar(jarPath: Path, action: (PluginJar) -> T): T? {
    return try {
      PluginJar(jarPath, fileSystemProvider).use { jar ->
        action(jar)
      }
    } catch (e: JarArchiveCannotBeOpenException) {
      LOG.warn("Unable to open $jarPath: " + e.cause?.message)
      null
    }
  }

  /**
   * Indicates if a path corresponds to a plugin descriptor.
   * 1. It is either `META-INF/plugin.xml`.
   * 2. Alternatively, it is a Plugin Model V2 module descriptor that occurs in the root of a plugin JAR.
   */
  private fun Path.isDescriptor(): Boolean {
    return when (nameCount) {
      1 -> getName(0).isPluginDescriptorInJarRoot()
      2 -> isMetaInfPluginXml()
      else -> false
    }
  }

  private fun Path.isMetaInfPluginXml() = META_INF_PLUGIN_XML_PATH_COMPONENTS == map { it.toString() }

  private fun Path.isPluginDescriptorInJarRoot(): Boolean {
    val isXml = hasExtension(XML_EXTENSION)
    return isXml && ideaPluginXmlDetector.isPluginDescriptor(this)
  }

  private fun Path.getModuleName(): String {
    return this.fileName.toString().removeSuffix(".$XML_EXTENSION")
  }
}
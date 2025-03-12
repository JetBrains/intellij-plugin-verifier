package com.jetbrains.plugin.structure.intellij.plugin.module

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.listJars
import com.jetbrains.plugin.structure.intellij.plugin.LIB_DIRECTORY
import com.jetbrains.plugin.structure.intellij.plugin.descriptors.IdeaPluginXmlDetector
import com.jetbrains.plugin.structure.jar.PluginJar
import java.nio.file.Path


private const val MODULES_DIR = "modules"

class ContentModuleScanner {
  private val ideaPluginXmlDetector = IdeaPluginXmlDetector()

  fun getContentModules(pluginArtifact: Path) : ContentModules {
    val libDir = pluginArtifact.resolve(LIB_DIRECTORY)
    if (!libDir.exists()) {
      return ContentModules(pluginArtifact, emptyList())
    }
    val jarPaths = libDir.listJars()
    val moduleJarPaths = libDir.resolve(MODULES_DIR).listJars()
    val contentModules = (jarPaths + moduleJarPaths).flatMap { jarPath ->
      PluginJar(jarPath).use { jar ->
        val descriptorPaths = jar.resolveDescriptors { it.matches() }
        descriptorPaths.map {
          val moduleName = if (it.isMetaInfPluginXml()) {
            "ROOT"
          } else {
            it.getModuleName()
          }
          ContentModule(moduleName, jarPath, it)
        }
      }
    }

    return ContentModules(pluginArtifact, contentModules)
  }

  private fun Path.matches(): Boolean {
    return when (this.nameCount) {
      1 -> getName(0).isPluginDescriptorInJarRoot()
      2 -> isMetaInfPluginXml()
      else -> false
    }
  }

  private fun Path.matches(vararg pathComponents: String): Boolean {
    val thisComponents = map {
      it.toString()
    }
    return thisComponents == pathComponents.toList()
  }

  // TODO constant
  private fun Path.isMetaInfPluginXml(): Boolean = matches("META-INF", "plugin.xml")

  private fun Path.isPluginDescriptorInJarRoot(): Boolean {
    // TODO constant
    val isXml = toString().endsWith(".xml", true)
    return isXml && ideaPluginXmlDetector.isPluginDescriptor(this)
  }

  private fun Path.getModuleName(): String {
    // TODO constant
    return this.fileName.toString().removeSuffix(".xml")
  }
}
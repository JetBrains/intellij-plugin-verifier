package com.jetbrains.pluginverifier.core

import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginXmlUtil
import com.jetbrains.pluginverifier.misc.toSystemIndependentName

/**
 * @author Sergey Patrikeev
 */
class ClassesForCheckSelector {

  private companion object {
    const val COMPILE_SERVER_EXTENSION_POINT = "com.intellij.compileServer.plugin"
  }

  fun getClassesForCheck(plugin: IdePlugin, pluginResolver: Resolver): Iterator<String> {
    val referencedResolvers = getAllReferencedClasses(plugin).mapNotNull { pluginResolver.getClassLocation(it) }
    val compileServerResolvers = getCompileServerResolvers(plugin, pluginResolver)

    val allResolvers = (referencedResolvers + compileServerResolvers).distinct()
    val result = Resolver.createUnionResolver("Plugin classes for check", allResolvers)
    return if (result.isEmpty) pluginResolver.allClasses else result.allClasses
  }

  private fun getCompileServerResolvers(plugin: IdePlugin, pluginResolver: Resolver): List<Resolver> {
    val compileServerJars = plugin.extensions
        .get(COMPILE_SERVER_EXTENSION_POINT)
        .mapNotNull { it.getAttributeValue("classpath") }
        .flatMap { it.split(";") }
        .filter { it.endsWith(".jar") }

    val compileJarsPaths = compileServerJars.map { "lib/" + it.toSystemIndependentName().trim('/') }.toSet()

    if (compileServerJars.isNotEmpty()) {
      val jarFileResolvers = pluginResolver.eventualResolvers
          .filterIsInstance<JarFileResolver>()
          .filter { it.classPath.size == 1 }
      return jarFileResolvers.filter {
        val jarFile = it.classPath[0]
        val absolutePath = jarFile.absolutePath
        val isCompileServerJar = compileJarsPaths.any { absolutePath.endsWith(it) }
        isCompileServerJar
      }
    }
    return emptyList()
  }

  private fun getAllReferencedClasses(plugin: IdePlugin): Set<String> =
      PluginXmlUtil.getAllClassesReferencedFromXml(plugin) +
          plugin.optionalDescriptors.flatMap { PluginXmlUtil.getAllClassesReferencedFromXml(it.value) }


}
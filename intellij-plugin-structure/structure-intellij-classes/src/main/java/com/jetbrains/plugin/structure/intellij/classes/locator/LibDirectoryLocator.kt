package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class LibDirectoryLocator : IdePluginClassesLocator {
  override val locationKey: LocationKey = LibDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): Resolver? {
    val pluginLib = File(pluginFile, "lib")
    if (pluginLib.isDirectory) {
      val jars = JarsUtils.collectJars(pluginLib, { true }, false).toList()
      return JarsUtils.makeResolver(jars)
    }
    return null
  }

}
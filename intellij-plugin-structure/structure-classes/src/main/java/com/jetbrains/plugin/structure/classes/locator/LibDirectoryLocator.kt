package com.jetbrains.plugin.structure.classes.locator

import com.google.common.base.Predicates
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class LibDirectoryLocator : IdePluginClassesLocator {
  override fun findClasses(idePlugin: IdePlugin, pluginDirectory: File): Resolver {
    val pluginLib = File(pluginDirectory, "lib")
    if (pluginLib.isDirectory) {
      val jars = JarsUtils.collectJars(pluginLib, Predicates.alwaysTrue(), false).toList()
      return JarsUtils.makeResolver("Plugin `lib` jars: " + pluginLib.canonicalPath, jars)
    }
    return Resolver.getEmptyResolver()
  }

}
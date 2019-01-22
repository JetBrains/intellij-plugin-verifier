package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.utils.JarsUtils
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class LibDirectoryLocator(private val readMode: Resolver.ReadMode) : ClassesLocator {
  override val locationKey = LibDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): Resolver? {
    val pluginLib = File(pluginFile, "lib")
    if (pluginLib.isDirectory) {
      val jars = JarsUtils.collectJars(pluginLib, { true }, false).toList()
      return JarsUtils.makeResolver(readMode, jars)
    }
    return null
  }

}

object LibDirectoryKey : LocationKey {
  override val name: String = "lib directory"

  override fun getLocator(readMode: Resolver.ReadMode) = LibDirectoryLocator(readMode)
}
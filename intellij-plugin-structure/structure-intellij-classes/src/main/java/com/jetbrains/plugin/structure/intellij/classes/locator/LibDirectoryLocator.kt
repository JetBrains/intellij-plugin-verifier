package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildJarFileResolvers
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class LibDirectoryLocator(private val readMode: Resolver.ReadMode) : ClassesLocator {
  override val locationKey = LibDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): List<Resolver> {
    val pluginLib = pluginFile.resolve("lib")
    if (pluginLib.isDirectory) {
      val jars = pluginLib.listFiles { file -> file.isJar() }.orEmpty().toList()
      return buildJarFileResolvers(jars, readMode, PluginFileOrigin.LibDirectory(idePlugin))
    }
    return emptyList()
  }

}

object LibDirectoryKey : LocationKey {
  override val name: String = "lib directory"

  override fun getLocator(readMode: Resolver.ReadMode) = LibDirectoryLocator(readMode)
}
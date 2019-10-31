package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildDirectoriesResolvers
import com.jetbrains.plugin.structure.classes.resolvers.buildJarOrZipFileResolvers
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class LibDirectoryLocator(private val readMode: Resolver.ReadMode) : ClassesLocator {
  override val locationKey = LibDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): List<Resolver> {
    val pluginLib = pluginFile.resolve("lib")
    val resolvers = arrayListOf<Resolver>()
    if (pluginLib.isDirectory) {
      val libDirectoryOrigin = PluginFileOrigin.LibDirectory(idePlugin)
      val jarsOrZips = pluginLib.listFiles { file -> file.isJar() || file.isZip() }.orEmpty().toList()
      val directories = pluginLib.listFiles { file -> file.isDirectory }.orEmpty().map { it.toPath() }
      resolvers.closeOnException {
        resolvers += buildJarOrZipFileResolvers(jarsOrZips, readMode, libDirectoryOrigin)
        resolvers += buildDirectoriesResolvers(directories, readMode, libDirectoryOrigin)
      }
    }
    return resolvers
  }

}

object LibDirectoryKey : LocationKey {
  override val name: String = "lib directory"

  override fun getLocator(readMode: Resolver.ReadMode) = LibDirectoryLocator(readMode)
}
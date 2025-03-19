/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.closeOnException
import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.base.utils.isZip
import com.jetbrains.plugin.structure.base.utils.listFiles
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.classes.resolvers.buildDirectoriesResolvers
import com.jetbrains.plugin.structure.classes.resolvers.buildJarOrZipFileResolvers
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.nio.file.Path

class LibDirectoryLocator(
  private val readMode: Resolver.ReadMode,
  private val fileOriginProvider: FileOriginProvider = LibDirectoryOriginProvider,
  private val libDirectoryFilter: LibDirectoryFilter = AnyLibDirectoryFilter
) : ClassesLocator {
  override val locationKey = LibDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: Path): List<Resolver> {
    val pluginLib = pluginFile.resolve("lib")
    val resolvers = arrayListOf<Resolver>()
    if (pluginLib.isDirectory) {
      val libDirectoryOrigin = fileOriginProvider.getFileOrigin(idePlugin, pluginFile)
      val pluginLibChildren = pluginLib.listFiles()
      val jarsOrZips = pluginLibChildren.filter { file -> file.isJar() || file.isZip() }
      val directories = pluginLibChildren
        .filter { file -> file.isDirectory }
        .filter(libDirectoryFilter::accept)
      resolvers.closeOnException {
        resolvers += buildJarOrZipFileResolvers(jarsOrZips, readMode, libDirectoryOrigin)
        resolvers += buildDirectoriesResolvers(directories, readMode, libDirectoryOrigin)
      }
    }
    return resolvers
  }

  fun interface LibDirectoryFilter {
    fun accept(file: Path): Boolean
  }

  object AnyLibDirectoryFilter : LibDirectoryFilter {
    override fun accept(file: Path) = true
  }
}

object LibDirectoryKey : LocationKey {
  override val name: String = "lib directory"

  override fun getLocator(readMode: Resolver.ReadMode) = LibDirectoryLocator(readMode)
}

object LibDirectoryOriginProvider: FileOriginProvider {
  override fun getFileOrigin(idePlugin: IdePlugin, pluginFile: Path) = PluginFileOrigin.LibDirectory(idePlugin)
}
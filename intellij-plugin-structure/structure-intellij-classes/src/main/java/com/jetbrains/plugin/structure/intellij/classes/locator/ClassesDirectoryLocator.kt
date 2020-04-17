/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.DirectoryResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File
import java.nio.file.Files

class ClassesDirectoryLocator(private val readMode: Resolver.ReadMode) : ClassesLocator {
  override val locationKey: LocationKey = ClassesDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): List<Resolver> {
    val classesDir = pluginFile.toPath().resolve("classes")
    if (Files.isDirectory(classesDir)) {
      val classFileOrigin = PluginFileOrigin.ClassesDirectory(idePlugin)
      return listOf(DirectoryResolver(classesDir, classFileOrigin, readMode))
    }
    return emptyList()
  }
}

object ClassesDirectoryKey : LocationKey {
  override val name: String = "classes directory"

  override fun getLocator(readMode: Resolver.ReadMode) = ClassesDirectoryLocator(readMode)
}

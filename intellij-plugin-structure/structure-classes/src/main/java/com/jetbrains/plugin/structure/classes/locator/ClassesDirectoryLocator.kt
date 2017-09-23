package com.jetbrains.plugin.structure.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.FilesResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class ClassesDirectoryLocator : IdePluginClassesLocator {
  override fun findClasses(idePlugin: IdePlugin, pluginDirectory: File): List<Resolver> {
    val classesDir = File(pluginDirectory, "classes")
    if (classesDir.isDirectory) {
      return listOf(FilesResolver("Plugin `classes` directory", classesDir))
    }
    return emptyList()
  }
}
package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.classes.resolvers.ClassFilesResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File
import java.nio.file.Files

class ClassesDirectoryLocator(private val readMode: Resolver.ReadMode) : ClassesLocator {
  override val locationKey: LocationKey = ClassesDirectoryKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): Resolver? {
    val classesDir = pluginFile.toPath().resolve("classes")
    if (Files.isDirectory(classesDir)) {
      return ClassFilesResolver(classesDir, readMode)
    }
    return null
  }
}

object ClassesDirectoryKey : LocationKey {
  override val name: String = "classes directory"

  override fun getLocator(readMode: Resolver.ReadMode) = ClassesDirectoryLocator(readMode)
}

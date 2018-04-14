package com.jetbrains.plugin.structure.intellij.classes.locator

import com.jetbrains.plugin.structure.base.utils.isJar
import com.jetbrains.plugin.structure.classes.resolvers.JarFileResolver
import com.jetbrains.plugin.structure.classes.resolvers.Resolver
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import java.io.File

class JarPluginLocator : ClassesLocator {
  override val locationKey: LocationKey = JarPluginKey

  override fun findClasses(idePlugin: IdePlugin, pluginFile: File): Resolver? {
    if (pluginFile.isJar()) {
      return JarFileResolver(pluginFile)
    }
    return null
  }
}

object JarPluginKey : LocationKey {
  override val name: String = "jar"

  override val locator: ClassesLocator = JarPluginLocator()
}